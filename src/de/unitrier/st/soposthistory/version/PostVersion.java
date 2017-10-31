package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpanVersion;
import de.unitrier.st.soposthistory.urls.Link;
import de.unitrier.st.soposthistory.urls.PostVersionUrl;
import de.unitrier.st.soposthistory.util.Config;
import org.hibernate.StatelessSession;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.insertList;
import static de.unitrier.st.soposthistory.util.Util.updateList;

@Entity
@Table(name = "PostVersion", schema = "stackoverflow16_12")
public class PostVersion {
    // database
    private int id;
    private Integer postId;
    private Integer postHistoryId;
    private Integer postTypeId;
    private Integer predPostHistoryId;
    private Integer succPostHistoryId;
    // internal
    private List<PostBlockVersion> postBlocks;
    private List<PostVersionUrl> urls;
    private PostVersion pred;
    private PostVersion succ;
    private boolean processed; // used to determine of post history has already been processed

    public PostVersion() {
        // database
        this.postId = null;
        this.postTypeId = null;
        this.postHistoryId = null;
        // database + internal
        this.reset();
    }

    public PostVersion(Integer postId, Integer postHistoryId, Integer postTypeId) {
        this();
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postTypeId = postTypeId;
        this.postBlocks = new LinkedList<>();
    }

    // reset data set in PostVersionList.processVersionHistory (needed for metrics comparison)
    public void reset() {
        // reset everything except for post id, post history id, post type id, and post blocks
        this.predPostHistoryId = null;
        this.succPostHistoryId = null;
        // internal
        this.urls = new LinkedList<>();
        this.pred = null;
        this.succ = null;
        this.processed = false;
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

    @Transient
    public PostVersion getPred() {
        return pred;
    }

    public void setPred(PostVersion pred) {
        this.pred = pred;
    }

    @Transient
    public PostVersion getSucc() {
        return succ;
    }

    @Transient
    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public void setSucc(PostVersion succ) {
        this.succ = succ;
    }

    public void insertPostBlocks(StatelessSession session) {
        insertList(session, postBlocks);
    }

    public void updateBlocks(StatelessSession session) {
        updateList(session, postBlocks);
    }

    public void extractUrlsFromTextBlocks() {
        for (TextBlockVersion currentTextBlock : getTextBlocks()) {
            List<Link> extractedLinks = Link.extract(currentTextBlock.getContent());
            for (Link currentLink : extractedLinks) {
                urls.add(new PostVersionUrl(postId, postHistoryId, currentTextBlock.getId(), currentLink.getUrl()));
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
     * @param config Configuration with similarity metrics and thresholds
     * @param <T> Either TextBlockVersion or CodeBlockVersion
     * @return Map with matched predecessor post blocks and number of times they were matched
     */
    public <T extends PostBlockVersion> Map<PostBlockVersion, Integer> findMatchingPredecessors(
                                                List<T> currentVersionPostBlocks,
                                                List<T> previousVersionPostBlocks,
                                                Config config) {

        Map<PostBlockVersion, Integer> matchedPredecessors = new HashMap<>();

        for (T currentVersionPostBlock : currentVersionPostBlocks) {
            List<PostBlockVersion> currentMatchedPredecessors
                    = currentVersionPostBlock.findMatchingPredecessors(previousVersionPostBlocks, config);

            for (PostBlockVersion matchedPredecessor : currentMatchedPredecessors) {
                matchedPredecessors.put(matchedPredecessor,
                        matchedPredecessors.getOrDefault(matchedPredecessor, 0) + 1);
            }
        }

        return matchedPredecessors;
    }

    public Set<PostBlockConnection> getConnections() {
        return getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public Set<PostBlockConnection> getConnections(Set<Integer> postBlockTypeFilter) {
        if (!processed) {
            throw new IllegalStateException("Post history has not been processed yet.");
        }

        HashSet<PostBlockConnection> connections = new HashSet<>();

        for (PostBlockVersion currentBlock : postBlocks) {
            if (currentBlock.isSelected(postBlockTypeFilter) && currentBlock.getPred() != null) {
                PostBlockVersion predBlock = currentBlock.getPred();
                Integer predBlockLocalId = predBlock.getLocalId();
                Integer predBlockPredLocalId = predBlock.getPred() != null ? predBlock.getPred().getLocalId() : null;
                Integer succBlockLocalId = currentBlock.getSucc() != null ? currentBlock.getSucc().getLocalId() : null;

                PostBlockLifeSpanVersion currentLifeSpanVersion = new PostBlockLifeSpanVersion(
                        postId, postHistoryId, currentBlock.getPostBlockTypeId(), currentBlock.getLocalId(),
                        predBlockLocalId, succBlockLocalId
                );

                PostBlockLifeSpanVersion predLifeSpanVersion = new PostBlockLifeSpanVersion(
                        postId, predBlock.getPostHistoryId(), predBlock.getPostBlockTypeId(), predBlock.getLocalId(),
                        predBlockPredLocalId, currentBlock.getLocalId()
                );

                connections.add(new PostBlockConnection(predLifeSpanVersion, currentLifeSpanVersion));
            }
        }

        return connections;
    }

    public int getPossibleConnections() {
        return getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleConnections(Set<Integer> postBlockTypeFilter) {
        if (!isProcessed()) {
            throw new IllegalStateException("Post history has not been processed yet.");
        }

        return getPossibleConnections(this.pred, postBlockTypeFilter);
    }

    public int getPossibleConnections(PostVersion pred, Set<Integer> postBlockTypeFilter) {
        // this also works if the post history has not been processed yet (meaning pred is not set yet for this PostVersion)
        int possibleConnections = 0;
        if (pred != null) {
            if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                int textBlockCount = getTextBlocks().size();
                int predTextBlockCount = pred.getTextBlocks().size();
                possibleConnections += textBlockCount * predTextBlockCount;
            }
            if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
                int codeBlockCount = getCodeBlocks().size();
                int predCodeBlockCount = pred.getCodeBlocks().size();
                possibleConnections += codeBlockCount * predCodeBlockCount;
            }
        }
        return possibleConnections;
    }

    @Override
    public String toString() {
        return "PostVersion: PostId=" + postId + ", PostHistoryId=" + postHistoryId;
    }
}
