package org.sotorrent.posthistoryextractor.version;

import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.StackSnippetVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.gt.PostBlockConnection;
import org.sotorrent.posthistoryextractor.gt.PostBlockLifeSpanVersion;
import org.sotorrent.posthistoryextractor.urls.Link;
import org.sotorrent.posthistoryextractor.urls.PostVersionUrl;
import org.hibernate.StatelessSession;
import org.sotorrent.util.HibernateUtils;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Entity
@Table(name="PostVersion")
public class PostVersion extends Version {
    private static final Pattern nonWordPattern = Pattern.compile("[^\\w]+");
    // patterns for generated comments
    // (to build those pattern, we iteratively retrieved  edit comments from table PostHistory, looked for bot-edits,
    // updated the regular expressions, and retrieved edit comments again)
    private static final Pattern gracePeriodPattern = Pattern.compile("edit removed during grace period", Pattern.CASE_INSENSITIVE);
    private static final Pattern editedTitleBodyTagsPattern = Pattern.compile("edited (title|body|tags)", Pattern.CASE_INSENSITIVE);
    private static final Pattern addedDeletedBodyPattern = Pattern.compile("(added|deleted) \\d+ character(s)? in body", Pattern.CASE_INSENSITIVE);
    private static final Pattern replacedHttpPattern = Pattern.compile("replaced http://.+ with https://.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern deprecatedTagPattern = Pattern.compile("removed deprecated tag", Pattern.CASE_INSENSITIVE);
    private static final Pattern nothingToDoPattern = Pattern.compile("\\[.+] has nothing to do with \\[.+]", Pattern.CASE_INSENSITIVE);
    private static final Pattern readBeforePattern = Pattern.compile("please read this before (rolling back|rejecting)", Pattern.CASE_INSENSITIVE);
    private static final Pattern rollbackPattern = Pattern.compile("rollback to .+", Pattern.CASE_INSENSITIVE);

    private static final Pattern[] generatedCommentPatterns = {
            gracePeriodPattern, editedTitleBodyTagsPattern, addedDeletedBodyPattern, replacedHttpPattern,
            deprecatedTagPattern, nothingToDoPattern, readBeforePattern, rollbackPattern
    };

    // database
    // see superclass members
    private Integer predPostHistoryId;
    private Integer succPostHistoryId;
    private String comment;
    // internal
    private List<PostBlockVersion> postBlocks;
    private List<StackSnippetVersion> stackSnippets;
    private List<PostVersionUrl> urls;
    private PostVersion pred;
    private PostVersion succ;

    public PostVersion() {
        // database
        this.postId = null;
        this.postTypeId = null;
        this.postHistoryId = null;
        this.postHistoryTypeId = null;
        this.creationDate = null;
        this.predPostHistoryId = null;
        this.succPostHistoryId = null;
        this.mostRecentVersion = false;
        this.comment = null;
        // internal
        this.pred = null;
        this.succ = null;
        this.urls = new LinkedList<>();
    }

    public PostVersion(Integer postId, Byte postTypeId,
                       Integer postHistoryId, Byte postHistoryTypeId,
                       Timestamp creationDate, String comment) {
        this();
        this.postId = postId;
        this.postTypeId = postTypeId;
        this.postHistoryId = postHistoryId;
        this.postHistoryTypeId = postHistoryTypeId;
        this.creationDate = creationDate;
        setComment(comment);
        this.postBlocks = new LinkedList<>();
        this.stackSnippets = new LinkedList<>();
    }

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "PostId")
    @Override
    public Integer getPostId() {
        return postId;
    }

    @Override
    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "PostTypeId")
    @Override
    public Byte getPostTypeId() {
        return postTypeId;
    }

    @Override
    public void setPostTypeId(Byte postTypeId) {
        this.postTypeId = postTypeId;
    }

    @Basic
    @Column(name = "PostHistoryId")
    @Override
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    @Override
    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    @Basic
    @Column(name = "PostHistoryTypeId")
    @Override
    public Byte getPostHistoryTypeId() {
        return postHistoryTypeId;
    }

    @Override
    public void setPostHistoryTypeId(Byte postHistoryTypeId) {
        this.postHistoryTypeId = postHistoryTypeId;
    }

    @Basic
    @Column(name = "CreationDate")
    @Override
    public Timestamp getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
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

    @Basic
    @Column(name = "MostRecentVersion")
    public boolean isMostRecentVersion() {
        return mostRecentVersion;
    }

    public void setMostRecentVersion(boolean mostRecentVersion) {
        this.mostRecentVersion = mostRecentVersion;
    }

    @Basic
    @Column(name = "Comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        // exclude empty comments (no word characters)
        if (comment == null || nonWordPattern.matcher(comment).replaceAll("").length() == 0) {
            return;
        }

        // ignore generated comments
        for (Pattern pattern : generatedCommentPatterns) {
            if (pattern.matcher(comment).find()) {
                return;
            }
        }

        this.comment = comment;
    }

    public void addPostBlockList(List<PostBlockVersion> postBlockList) {
        postBlocks.addAll(postBlockList);
    }

    public void addStackSnippetList(List<StackSnippetVersion> stackSnippetList) {
        for (StackSnippetVersion stackSnippet : stackSnippetList) {
            stackSnippet.setPostId(postId);
            stackSnippet.setPostTypeId(postTypeId);
            stackSnippet.setPostHistoryId(postHistoryId);
            stackSnippets.add(stackSnippet);
        }
    }

    public List<PostBlockVersion> sortPostBlocks() {
        postBlocks.sort(Comparator.comparing(PostBlockVersion::getLocalId));
        return postBlocks;
    }

    @Transient
    public List<PostBlockVersion> getPostBlocks() {
        return postBlocks;
    }

    @Transient
    public List<StackSnippetVersion> getStackSnippets() {
        return stackSnippets;
    }

    @Transient
    public List<PostBlockVersion> getPostBlocks(Set<Byte> postBlockTypeFilter) {
        return postBlocks
                .stream()
                .filter(b -> b.isSelected(postBlockTypeFilter))
                .collect(Collectors.toList());
    }

    @Transient
    public List<CodeBlockVersion> getCodeBlocks() {
        return getPostBlocks(CodeBlockVersion.getPostBlockTypeIdFilter())
                .stream()
                .map(b -> (CodeBlockVersion) b)
                .collect(Collectors.toList());
    }

    @Transient
    public List<TextBlockVersion> getTextBlocks() {
        return getPostBlocks(TextBlockVersion.getPostBlockTypeIdFilter())
                .stream()
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

    public void setSucc(PostVersion succ) {
        this.succ = succ;
    }

    public void insertStackSnippets(StatelessSession session) {
        HibernateUtils.insertList(session, stackSnippets);
    }

    public void insertPostBlocks(StatelessSession session) {
        HibernateUtils.insertList(session, postBlocks);
    }

    public void updateBlocks(StatelessSession session) {
        HibernateUtils.updateList(session, postBlocks);
    }

    public void extractUrlsFromTextBlocks() {
        for (TextBlockVersion currentTextBlock : getTextBlocks()) {
            List<Link> extractedLinks = Link.extractTyped(currentTextBlock.getContent());
            for (Link currentLink : extractedLinks) {
                urls.add(
                        new PostVersionUrl(postId, postHistoryId, currentTextBlock.getId(), currentLink, getContent())
                );
            }
        }
    }

    public void insertUrls(StatelessSession session) {
        HibernateUtils.insertList(session, urls);
    }

    /**
     * Compare all text or code blocks from this version to all text or blocks from the previous version
     * Rules for matching: (1) equality of content, (2) similarity metric, (3) order in post.
     * @param currentVersionPostBlocks Text or code blocks from current post version
     * @param previousVersionPostBlocks Text or code blocks from previous post version
     * @param config Configuration with similarity metrics and thresholds
     * @param postBlockTypeFilter IDs of selected post block types
     */
    public void findMatchingPredecessors(List<PostBlockVersion> currentVersionPostBlocks,
                                         List<PostBlockVersion> previousVersionPostBlocks,
                                         Config config,
                                         Set<Byte> postBlockTypeFilter) {

        for (PostBlockVersion currentVersionPostBlock : currentVersionPostBlocks) {
            // apply post block type filter
            if (!currentVersionPostBlock.isSelected(postBlockTypeFilter)) {
                continue;
            }

            List<PostBlockVersion> currentPostBlockMatchingSuccessorsPreviousVersion =
                    currentVersionPostBlock.findMatchingPredecessors(
                            previousVersionPostBlocks, config, postBlockTypeFilter);

            // add this post block as matching successor for all matching predecessors and add corresponding similarity
            for (PostBlockVersion matchingPredecessor : currentPostBlockMatchingSuccessorsPreviousVersion) {
                matchingPredecessor.getMatchingSuccessors().add(currentVersionPostBlock);
                matchingPredecessor.getSuccessorSimilarities().put(
                        currentVersionPostBlock,
                        currentVersionPostBlock.getPredecessorSimilarities().get(matchingPredecessor)
                );
            }
        }
    }

    // reset data set in PostVersionList.processVersionHistory (needed for metrics comparison)
    public void resetPostBlockVersionHistory() {
        for (PostBlockVersion currentPostBlockVersion : postBlocks) {
            currentPostBlockVersion.resetVersionHistory();
        }
    }

    @Transient
    public Set<PostBlockConnection> getConnections() {
        return getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    @Transient
    public Set<PostBlockConnection> getConnections(Set<Byte> postBlockTypeFilter) {
        HashSet<PostBlockConnection> connections = new HashSet<>();

        for (PostBlockVersion currentBlock : postBlocks) {
            // wrong post block type or first version --> no connections
            if (!currentBlock.isSelected(postBlockTypeFilter) || currentBlock.getPred() == null) {
                continue;
            }

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

        return connections;
    }

    @Transient
    public int getPossibleComparisons() {
        return getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    @Transient
    public int getPossibleComparisons(Set<Byte> postBlockTypeFilter) {
        // this only works if the post version list has already been sorted (meaning pred is set for this PostVersion)

        // first version cannot have comparisons
        if (pred == null) {
            return 0;
        }

        int possibleComparisons = 0;

        // text blocks
        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
            int textBlockCount = getTextBlocks().size();
            int predTextBlockCount = pred.getTextBlocks().size();
            possibleComparisons += textBlockCount * predTextBlockCount;
        }

        // code blocks
        if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
            int codeBlockCount = getCodeBlocks().size();
            int predCodeBlockCount = pred.getCodeBlocks().size();
            possibleComparisons += codeBlockCount * predCodeBlockCount;
        }

        return possibleComparisons;
    }

    @Transient
    public int getPossibleConnections() {
        return getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    @Transient
    public int getPossibleConnections(Set<Byte> postBlockTypeFilter) {
        // this only works if the post version list has already been sorted (meaning pred is set for this PostVersion)

        // first version cannot have connections
        if (pred == null) {
            return 0;
        } else {
            return getPostBlocks(postBlockTypeFilter).size();
        }
    }

    @Transient
    public int getFailedPredecessorComparisons() {
        return getFailedPredecessorComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    @Transient
    public int getFailedPredecessorComparisons(Set<Byte> postBlockTypeFilter) {
        int sum = 0;
        for (PostBlockVersion postBlockVersion : postBlocks) {
            sum += postBlockVersion.getFailedPredecessorsComparisons(postBlockTypeFilter).size();
        }
        return sum;
    }

    @Override
    public String toString() {
        return "PostVersion: PostId=" + postId + ", PostHistoryId=" + postHistoryId;
    }
}
