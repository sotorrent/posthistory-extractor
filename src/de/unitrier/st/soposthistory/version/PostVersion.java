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
     * @param <T> Either TextBlockVersion or CodeBlockVersion
     */
    public <T extends PostBlockVersion> Map<PostBlockVersion, Integer> findMatchingPredecessors(
                                                List<T> currentVersionPostBlocks,
                                                List<T> previousVersionPostBlocks) {

        Map<PostBlockVersion, Integer> matchedPredecessors = new HashMap<>();

        for (T currentVersionPostBlock : currentVersionPostBlocks) {
            List<PostBlockVersion> currentMatchedPredecessors
                    = currentVersionPostBlock.findMatchingPredecessors(previousVersionPostBlocks);

            for (PostBlockVersion matchedPredecessor : currentMatchedPredecessors) {
                matchedPredecessors.put(matchedPredecessor,
                        matchedPredecessors.getOrDefault(matchedPredecessor, 0) + 1);
            }
        }

        return matchedPredecessors;
    }

    @Override
    public String toString() {
        return "PostVersion: PostId=" + postId + ", PostHistoryId=" + postHistoryId;
    }
}
