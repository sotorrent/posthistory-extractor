package org.sotorrent.posthistoryextractor.version;

import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.diffs.PostBlockDiffList;
import org.sotorrent.posthistoryextractor.gt.PostBlockConnection;
import org.sotorrent.posthistoryextractor.gt.PostBlockLifeSpan;
import org.sotorrent.posthistoryextractor.history.PostHistory;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.urls.Link;
import org.hibernate.StatelessSession;
import org.sotorrent.util.FileUtils;
import org.sotorrent.util.LogUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class PostVersionList extends LinkedList<PostVersion> implements VersionList {
    private static Logger logger = null;

    private int postId;
    private byte postTypeId;
    private boolean sorted;

    private PostBlockDiffList diffs;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(PostVersionList.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PostVersionList(int postId, byte postTypeId) {
        super();
        this.postId = postId;
        this.postTypeId = postTypeId;
        this.sorted = false;
        this.diffs = new PostBlockDiffList();
    }

    // resetPostBlockVersionHistory data set in PostVersionList.processVersionHistory (needed for metrics comparison)
    public void resetPostBlockVersionHistory() {
        this.diffs = new PostBlockDiffList();
        for (PostVersion currentVersion : this) {
            currentVersion.resetPostBlockVersionHistory();
        }
    }

    public boolean isSorted() {
        return sorted;
    }

    public static PostVersionList readFromCSV(Path dir, int postId, byte postTypeId) {
        return readFromCSV(dir, postId, postTypeId, true);
    }

    public static PostVersionList readFromCSV(Path dir, int postId, byte postTypeId, boolean processVersionHistory) {
        // read post history
        List<PostHistory> postHistoryList = PostHistory.readFromCSV(dir, postId, PostHistory.contentPostHistoryTypes);

        // convert to post version list
        PostVersionList postVersionList = new PostVersionList(postId, postTypeId);
        for (PostHistory postHistory : postHistoryList) {
            postHistory.extractPostBlocks();
            PostVersion postVersion = postHistory.toPostVersion(postTypeId);
            postVersion.extractUrlsFromTextBlocks();
            postVersionList.add(postVersion);
        }

        // sort list according to CreationDate, because order in CSV may not be chronologically
        postVersionList.sort();

        if (processVersionHistory) {
            postVersionList.processVersionHistory();
        }

        return postVersionList;
    }

    public static List<PostVersionList> readFromDirectory(Path dir) {
        return readFromDirectory(dir, true);
    }

    public static List<PostVersionList> readFromDirectory(Path dir, boolean processVersionHistory) {
        return FileUtils.processFiles(dir,
                file -> PostHistory.fileNamePattern.matcher(file.toFile().getName()).matches(),
                file -> PostVersionList.readFromCSV(
                        dir,
                        Integer.parseInt(file.toFile().getName().replace(".csv", "")),
                        Posts.UNKNOWN_ID, // cannot determine this from file name or file content
                        processVersionHistory
                )
        );
    }

    public void sort() {
        // empty list is already sorted
        if (this.size() == 0) {
            return;
        }

        // sort versions according to their creation date
        this.sort(Comparator.comparing(PostVersion::getCreationDate));

        // set predecessors and successors
        for (int i=1; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);
            PostVersion pred = this.get(i-1);
            currentVersion.setPred(pred);
            currentVersion.setPredPostHistoryId(pred.getPostHistoryId());
            pred.setSucc(currentVersion);
            pred.setSuccPostHistoryId(currentVersion.getPostHistoryId());
        }

        this.sorted = true;

        // mark most recent post version and post block versions
        this.getLast().setMostRecentVersion(true);
        this.getLast().getPostBlocks().forEach(
                postBlock -> postBlock.setMostRecentVersion(true)
        );
    }

    public void processVersionHistory() {
        processVersionHistory(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public void processVersionHistory(Set<Byte> postBlockTypeFilter) {
        processVersionHistory(Config.DEFAULT, postBlockTypeFilter);
    }

    public void processVersionHistory(Config config) {
        processVersionHistory(config, PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    /**
     * Match post blocks between two versions, set root post blocks, and compute the diffs between two matched versions
     * of a post block.
     *
     * @param config Configuration with similarity metrics and thresholds
     * @param postBlockTypeFilter Set of postBlockTypeIds (1 for text blocks, 2 for code blocks), mainly needed for evaluation of similarity metrics
     */
    public void processVersionHistory(Config config, Set<Byte> postBlockTypeFilter) {
        // list must be sorted (in particular the pred and succ references must be set)
        if (!this.isSorted()) {
            this.sort();
        }

        for (int i=0; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);

            if (config.getExtractUrls() && postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                currentVersion.extractUrlsFromTextBlocks();
            }

            int predIndex = i-1;
            int succIndex = i+1;

            if (predIndex == -1) {
                // currentVersion is first element
                if (currentVersion.getPred() != null || currentVersion.getPredPostHistoryId() != null) {
                    String msg = "First element has predecessor : " + currentVersion;
                    logger.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // the post blocks in the first version have themselves as root post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    currentPostBlock.setRootPostBlock(currentPostBlock);
                }
            } else {
                // currentVersion >= second element
                PostVersion previousVersion = this.get(predIndex);
                if (!currentVersion.getPred().equals(this.get(predIndex))
                        || !currentVersion.getPredPostHistoryId().equals(this.get(predIndex).getPostHistoryId())) {
                    String msg = "Wrong predecessor set for " + currentVersion;
                    logger.warning(msg);
                    throw new IllegalStateException(msg);
                }

                // find matching predecessors for text and code blocks by (1) equality of content and (2) similarity metric
                // and set list of successors in previous version
                currentVersion.findMatchingPredecessors(
                    currentVersion.getPostBlocks(),
                    previousVersion.getPostBlocks(),
                    config,
                    postBlockTypeFilter
                );

                // set predecessors of text and code blocks if
                //   (1) only one matching predecessor exists or
                //   (2) multiple matching predecessors exist, but only one of them is equal,
                // and that candidate is only matched by one block in the current version
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    currentPostBlock.setUniqueMatchingPred();
                }

                // next, try to set remaining predecessors using context (neighboring blocks of post block and matching predecessor)

                // first, consider predecessors of blocks above and below as context
                boolean matched = true;
                while (matched) {
                    matched = setPostBlockPredContext(currentVersion, previousVersion, postBlockTypeFilter,
                            PostBlockVersion.MatchingStrategy.BOTH);
                }
                // then, consider only block below OR above as context
                matched = true;
                while (matched) {
                    // text blocks are more likely to be captions of code blocks below -> choose BELOW first
                    matched = setPostBlockPredContext(currentVersion, previousVersion, postBlockTypeFilter,
                            PostBlockVersion.MatchingStrategy.BELOW);
                }
                matched = true;
                while (matched) {
                    matched = setPostBlockPredContext(currentVersion, previousVersion, postBlockTypeFilter,
                            PostBlockVersion.MatchingStrategy.ABOVE);
                }

                // set the remaining predecessors using the order in the post (localId)
                setPostBlockPredPosition(currentVersion, postBlockTypeFilter);

                // finally, try to set runner-up match in case position-based strategy failed
                setPostBlockPredRunnerUp(currentVersion, postBlockTypeFilter);

                // set root post block for all post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    if (currentPostBlock.getPred() == null) {
                        // block has no predecessor -> set itself as root post block
                        currentPostBlock.setRootPostBlock(currentPostBlock);
                    } else {
                        // block has predecessor -> set root post block of predecessor as root post block of this block
                        currentPostBlock.setRootPostBlock(currentPostBlock.getPred().getRootPostBlock());
                    }
                }
            }

            if (succIndex==this.size()) {
                // current is last element
                if (currentVersion.getSucc() != null || currentVersion.getSuccPostHistoryId() != null) {
                    String msg = "Wrong successor set for " + currentVersion + " (last element)";
                    logger.warning(msg);
                    throw new IllegalStateException(msg);
                }
            } else {
                // current is not last element
                if (!currentVersion.getSucc().equals(this.get(succIndex))
                        || !currentVersion.getSuccPostHistoryId().equals(this.get(succIndex).getPostHistoryId())) {
                    String msg = "Wrong successor set for " + currentVersion;
                    logger.warning(msg);
                    throw new IllegalStateException(msg);
                }
            }
        }

        if (config.getComputeDiffs()) {
            // compute diffs
            diffs.fromPostVersionList(this);
        }
    }

    private boolean setPostBlockPredContext(PostVersion currentVersion,
                                           PostVersion previousVersion,
                                           Set<Byte> postBlockTypeFilter,
                                           PostBlockVersion.MatchingStrategy matchingStrategy) {

        for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
            boolean matched = currentPostBlock.setPredContext(currentVersion, previousVersion, matchingStrategy);
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private void setPostBlockPredPosition(PostVersion currentVersion,
                                          Set<Byte> postBlockTypeFilter) {

        for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
            if (currentPostBlock.getPred() == null) {
                // no matching predecessor found, try setting predecessor using position information
                currentPostBlock.setPredPosition();
            }
        }
    }

    private void setPostBlockPredRunnerUp(PostVersion currentVersion,
                                          Set<Byte> postBlockTypeFilter) {

        for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
            if (currentPostBlock.getPred() == null) {
                // no matching predecessor found, try predecessors with next-highest similarity
                currentPostBlock.setPredRunnerUp();
            }
        }
    }

    public PostBlockDiffList getDiffs() {
        return diffs;
    }

    public void insert(StatelessSession session) {
        for (PostVersion currentVersion : this) {
            // save current post version
            session.insert(currentVersion);
            // update all post blocks (pred, succ, similarity, root post block, etc.)
            for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                session.update(currentPostBlock);
            }
        }

        diffs.insert(session);
    }

    // Needed for GT-App to display all links correctly with Commonmark
    public void normalizeLinks() {
        for (PostVersion postVersion : this) {
            String mergedTextBlocks = postVersion.getMergedTextBlockContent();
            List<Link> extractedLinks = Link.extractTyped(mergedTextBlocks);

            for (TextBlockVersion currentTextBlock : postVersion.getTextBlocks()) {
                String normalizedMarkdownContent = Link.normalizeLinks(currentTextBlock.getContent(), extractedLinks);

                if (normalizedMarkdownContent.trim().isEmpty()) { // handles, e.g., post 3745432
                    postVersion.getPostBlocks().remove(currentTextBlock);
                } else {
                    currentTextBlock.setContent(normalizedMarkdownContent);
                }
            }
        }
    }

    public List<PostBlockLifeSpan> getPostBlockLifeSpans() {
        return getPostBlockLifeSpans(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public List<PostBlockLifeSpan> getPostBlockLifeSpans(Set<Byte> postBlockTypeFilter) {
        List<PostBlockLifeSpan> lifeSpans = new LinkedList<>();

        if (!this.sorted) {
            sort();
        }

        for (PostVersion currentPostVersion : this) {
            for (PostBlockVersion currentPostBlockVersion : currentPostVersion.getPostBlocks()) {
                // apply filter
                if (!currentPostBlockVersion.isSelected(postBlockTypeFilter)) {
                    continue;
                }

                // skip blocks that have previously been processed
                if (currentPostBlockVersion.isLifeSpanExtracted()) {
                    continue;
                }

                lifeSpans.add(PostBlockLifeSpan.fromPostBlockVersion(currentPostBlockVersion));
            }
        }

        // resetPostBlockVersionHistory flag "processed"
        for (PostVersion currentPostVersion : this) {
            for (PostBlockVersion currentPostBlockVersion : currentPostVersion.getPostBlocks()) {
                currentPostBlockVersion.setLifeSpanExtracted(false);
            }
        }

        return lifeSpans;
    }

    public Set<PostBlockConnection> getConnections() {
        return getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public Set<PostBlockConnection> getConnections(Set<Byte> postBlockTypeFilter) {
        Set<PostBlockConnection> connections = new HashSet<>();
        for (PostVersion currentVersion : this) {
            connections.addAll(currentVersion.getConnections(postBlockTypeFilter));
        }
        return connections;
    }

    public int getPossibleComparisons() {
        return getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleComparisons(Set<Byte> postBlockTypeFilter) {
        // we can only determine the possible comparisons if the list has been sorted and thus the predecessor references are set
        if (!this.isSorted()) {
            String msg = "Possible comparisons can only be determined if PostVersionList has been sorted.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
        int possibleComparisons = 0;
        for (int i=1; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);
            // this also works if post history has not been extracted yet
            possibleComparisons += currentVersion.getPossibleComparisons(postBlockTypeFilter);
        }
        return possibleComparisons;
    }

    public int getPossibleConnections() {
        return getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleConnections(Set<Byte> postBlockTypeFilter) {
        // we can only determine the possible comparisons if the list has been sorted, because first element cannot have connections
        if (!this.isSorted()) {
            String msg = "Possible connections can only be determined if PostVersionList has been sorted.";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }
        int possibleConnections = 0;
        for (int i=1; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);
            // this also works if post history has not been extracted yet
            possibleConnections += currentVersion.getPossibleConnections(postBlockTypeFilter);
        }
        return possibleConnections;
    }

    public int getPostBlockVersionCount() {
        return getPostBlockVersionCount(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getTextBlockVersionCount() {
        return getPostBlockVersionCount(TextBlockVersion.getPostBlockTypeIdFilter());
    }

    public int getCodeBlockVersionCount() {
        return getPostBlockVersionCount(CodeBlockVersion.getPostBlockTypeIdFilter());
    }

    public int getPostBlockVersionCount(Set<Byte> postBlockTypeFilter) {
        return Math.toIntExact(this.stream()
                .map(v -> v.getPostBlocks(postBlockTypeFilter))
                .mapToLong(List::size)
                .sum());
    }

    public List<Integer> getPostHistoryIds() {
        List<Integer> postHistoryIds = new ArrayList<>(this.size());
        for (PostVersion version : this) {
            postHistoryIds.add(version.getPostHistoryId());
        }
        return postHistoryIds;
    }

    public PostVersion getPostVersion(int postHistoryId) {
        for (PostVersion version : this) {
            if (version.getPostHistoryId() == postHistoryId) {
                return version;
            }
        }
        return null;
    }

    public int getPostId() {
        return postId;
    }

    public byte getPostTypeId() {
        return postTypeId;
    }

    public int getFailedPredecessorComparisons() {
        return getFailedPredecessorComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getFailedPredecessorComparisons(Set<Byte> postBlockTypeFilter) {
        int sum = 0;
        for (PostVersion version : this) {
            sum += version.getFailedPredecessorComparisons(postBlockTypeFilter);
        }
        return sum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PostVersion version : this) {
            sb.append(version.toString());
            sb.append("\n");
        }
        return "PostVersionList (PostId=" + postId + "):\n" + sb;
    }
}
