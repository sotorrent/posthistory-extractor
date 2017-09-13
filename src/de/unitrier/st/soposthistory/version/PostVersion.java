package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.urls.Link;
import de.unitrier.st.soposthistory.urls.PostVersionUrl;
import org.hibernate.StatelessSession;

import javax.persistence.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.insertList;
import static de.unitrier.st.soposthistory.util.Util.updateList;

@Entity
@Table(name = "PostVersion", schema = "stackoverflow16_12")
public class PostVersion {
    private final double EQUALITY_SIMILARITY = 10.0;
    // database
    private int id;
    private Integer postId;
    private Integer postHistoryId;
    private Integer postTypeId;
    private Integer predPostHistoryId;
    private Integer succPostHistoryId;
    // internal
    private final List<PostBlockVersion> postBlocks;
    private final List<PostVersionUrl> urls;

    public PostVersion() {
        // database
        this.postId = null;
        this.postTypeId = null;
        this.postHistoryId = null;
        this.predPostHistoryId = null;
        this.succPostHistoryId = null;
        // internal
        this.postBlocks = new LinkedList<>();
        this.urls = new LinkedList<>();
    }

    public PostVersion(Integer postId, Integer postHistoryId, Integer postTypeId) {
        this();
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postTypeId = postTypeId;
    }

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "PostId")
    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "PostTypeId")
    public Integer getPostTypeId() {
        return postTypeId;
    }

    public void setPostTypeId(Integer postTypeId) {
        this.postTypeId = postTypeId;
    }

    @Basic
    @Column(name = "PostHistoryId")
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    @Basic
    @Column(name = "PredPostHistoryId")
    public Integer getPredPostHistoryId() {
        return predPostHistoryId;
    }

    public void setPredPostHistoryId(Integer predPostHistoryId) {
        this.predPostHistoryId = predPostHistoryId;
    }

    @Basic
    @Column(name = "SuccPostHistoryId")
    public Integer getSuccPostHistoryId() {
        return succPostHistoryId;
    }

    public void setSuccPostHistoryId(Integer succPostHistoryId) {
        this.succPostHistoryId = succPostHistoryId;
    }

    @Transient
    public List<PostBlockVersion> getPostBlocks() {
        return postBlocks;
    }

    public void addPostBlock(PostBlockVersion block) {
        postBlocks.add(block);
    }

    public void addPostBlockList(List<PostBlockVersion> blockList) {
        for (PostBlockVersion block : blockList) {
            addPostBlock(block);
        }
    }

    public List<PostBlockVersion> sortPostBlocks() {
        postBlocks.sort((b1, b2) ->
                b1.getLocalId() < b2.getLocalId() ? -1 : b1.getLocalId() > b2.getLocalId() ? 1 : 0
        );
        return postBlocks;
    }

    @Transient
    public List<CodeBlockVersion> getCodeBlocks() {
        return postBlocks
                .stream()
                .filter(b -> b instanceof CodeBlockVersion)
                .map(b -> (CodeBlockVersion) b)
                .collect(Collectors.toList());
    }

    @Transient
    public List<TextBlockVersion> getTextBlocks() {
        return postBlocks
                .stream()
                .filter(b -> b instanceof TextBlockVersion)
                .map(b -> (TextBlockVersion) b)
                .collect(Collectors.toList());
    }

    @Transient
    public String getMergedTextBlockContent() {
        return postBlocks
                .stream()
                .filter(b -> b instanceof TextBlockVersion)
                .map(PostBlockVersion::getContent)
                .collect(Collectors.joining("\n"));
    }

    @Transient
    public String getContent() {
        return postBlocks
                .stream()
                .map(PostBlockVersion::getContent)
                .collect(Collectors.joining("\n"));
    }

    public void insertPostBlocks(StatelessSession session) {
        insertList(session, postBlocks);
    }

    public void updateBlocks(StatelessSession session) {
        updateList(session, postBlocks);
    }

    public void extractUrlsFromTextBlocks() {
        for (TextBlockVersion currentTextBlock : getTextBlocks()) {
            Matcher linkMatcher = Link.regex.matcher(currentTextBlock.getContent());
            while (linkMatcher.find()) {
                String url = linkMatcher.group(1);
                urls.add(new PostVersionUrl(postId, postHistoryId, currentTextBlock.getId(), url));
            }
        }
    }

    public void insertUrls(StatelessSession session) {
        insertList(session, urls);
    }

    /**
     * Compare all text or code blocks from this version to all text or blocks from the previous version
     * Rules for matching: (1) equality of content, (2) similarity metric, (3) order in post.
     * @param currentVersionPostBlocks Text or code blocks from current post version
     * @param previousVersionPostBlocks Text or code blocks from previous post version
     * @param similarityThreshold Similarity threshold for matching
     * @param <T> Either TextBlockVersion or CodeBlockVersion
     */
    public <T extends PostBlockVersion> void computePostBlockSimilarityAndDiffs(
                                                List<T> currentVersionPostBlocks,
                                                List<T> previousVersionPostBlocks,
                                                double similarityThreshold) {
        for (T currentVersionPostBlock : currentVersionPostBlocks) {
            HashMap<T, Double> similarities = new HashMap<>();
            double maxSimilarity = -1;

            for (T previousVersionPostBlock : previousVersionPostBlocks) {
                boolean equal = currentVersionPostBlock.getContent().equals(previousVersionPostBlock.getContent());
                double similarity = currentVersionPostBlock.compareTo(previousVersionPostBlock);

                if (equal) {
                    // equal predecessors have similarity 10.0 (see final constant)
                    similarities.put(previousVersionPostBlock, EQUALITY_SIMILARITY);
                    maxSimilarity = EQUALITY_SIMILARITY;
                } else {
                    similarities.put(previousVersionPostBlock, similarity);
                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                    }
                }
            }

            // set most similar post block as predecessor:
            // (1) equality of content, (2) similarity metric, (3) order in post.
            final double finalMaxSimilarity = maxSimilarity; // final value needed for lambda expression

            if (finalMaxSimilarity >= similarityThreshold) {
                // get predecessors with max. similarity
                List<PostBlockVersion> matchingPredecessors = similarities.entrySet()
                        .stream()
                        .filter(e -> e.getValue() == finalMaxSimilarity)
                        .sorted(Comparator.comparing(e -> e.getKey().getLocalId())) // TODO: asc or desc???
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                int pos = 0;
                while(pos < matchingPredecessors.size() && matchingPredecessors.get(pos).isPredOfBlock()){
                    pos++;
                }

                if (matchingPredecessors.size() > 0 && pos < matchingPredecessors.size()) {

                    // set predecessor for first match
                    if (finalMaxSimilarity == EQUALITY_SIMILARITY) {
                        currentVersionPostBlock.setPred(matchingPredecessors.get(pos), 1.0); // computes diff
                        currentVersionPostBlock.setPredEqual(true);
                    } else {
                        currentVersionPostBlock.setPred(matchingPredecessors.get(pos), finalMaxSimilarity); // computes diff
                        currentVersionPostBlock.setPredEqual(false);
                    }

                    matchingPredecessors.get(pos).setIsPredOfBlock(true);

                    // increase successor count for all matches
                    for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
                        currentVersionPostBlock.incrementPredCount();
                        matchingPredecessor.incrementSuccCount();
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostVersion that = (PostVersion) o;

        if (!postId.equals(that.getPostId())) return false;
        if (!postHistoryId.equals(that.getPostHistoryId())) return false;
        if (!postTypeId.equals(that.getPostTypeId())) return false;
        if (!predPostHistoryId.equals(that.getPredPostHistoryId())) return false;
        if (!succPostHistoryId.equals(that.getSuccPostHistoryId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = postId;
        result = 31 * result + postHistoryId;
        result = 31 * result + postTypeId;
        result = 31 * result + predPostHistoryId;
        result = 31 * result + succPostHistoryId;
        return result;
    }

    @Override
    public String toString() {
        return "PostVersion: PostId=" + postId + ", PostHistoryId=" + postHistoryId;
    }
}
