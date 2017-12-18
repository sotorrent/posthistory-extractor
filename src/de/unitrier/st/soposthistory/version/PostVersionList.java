package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.diffs.PostBlockDiffList;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpan;
import de.unitrier.st.soposthistory.history.PostHistory;
import de.unitrier.st.soposthistory.urls.Link;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostVersionList extends LinkedList<PostVersion> {
    //TODO: Add methods to extract history of code or text blocks (either ignoring versions where only blocks of the other type changed (global) or where one particular block did not change (local)

    public static final Pattern fileNamePattern = Pattern.compile("(\\d+)\\.csv");
    private static Logger logger = null;
    private static final CSVFormat csvFormatVersionList;

    private int postId;
    private int postTypeId;
    private boolean sorted;
    private PostBlockDiffList diffs;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(PostVersionList.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for ground truth
        csvFormatVersionList = CSVFormat.DEFAULT
                .withHeader("Id", "PostId", "UserId", "PostHistoryTypeId", "RevisionGUID", "CreationDate", "Text", "UserDisplayName", "Comment")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withFirstRecordAsHeader();
    }

    public PostVersionList(int postId, int postTypeId) {
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

    public static PostVersionList readFromCSV(Path dir, int postId, int postTypeId) {
        return readFromCSV(dir, postId, postTypeId, true);
    }

    public static PostVersionList readFromCSV(Path dir, int postId, int postTypeId, boolean processVersionHistory) {
        // ensure that input directory exists
        Util.ensureDirectoryExists(dir);

        PostVersionList postVersionList = new PostVersionList(postId, postTypeId);
        Path pathToCSVFile = Paths.get(dir.toString(), postId + ".csv");

        CSVParser parser;
        try {
            parser = CSVParser.parse(
                    pathToCSVFile.toFile(),
                    StandardCharsets.UTF_8,
                    csvFormatVersionList
            );
            parser.getHeaderMap();

            logger.info("Reading version data from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            List<CSVRecord> records = parser.getRecords();

            if (records.size() > 0) {
                for (CSVRecord record : records) {
                    byte postHistoryTypeId = Byte.parseByte(record.get("PostHistoryTypeId"));
                    // only consider relevant Post History Types
                    if (!PostHistory.relevantPostHistoryTypes.contains((int)postHistoryTypeId)) {
                        continue;
                    }

                    String text = record.get("Text");
                    // ignore entries where column "Text" is empty
                    if (text.isEmpty()) {
                        continue;
                    }

                    int id = Integer.parseInt(record.get("Id"));
                    assertEquals(postId, Integer.parseInt(record.get("PostId")));
                    String userId = record.get("UserId");
                    String revisionGuid = record.get("RevisionGUID");
                    Timestamp creationDate = Timestamp.valueOf(record.get("CreationDate"));
                    String userDisplayName = record.get("UserDisplayName");
                    String comment = record.get("Comment");

                    PostHistory postHistory = new PostHistory(
                            id, postId, postTypeId, userId, postHistoryTypeId, revisionGuid, creationDate,
                            text, userDisplayName, comment);
                    postHistory.extractPostBlocks();
                    PostVersion postVersion = postHistory.toPostVersion();
                    postVersion.extractUrlsFromTextBlocks();

                    postVersionList.add(postVersion);
                }
            }

            // sort list according to PostHistoryId, because order in CSV may not be chronologically
            postVersionList.sort();

            if (processVersionHistory) {
                postVersionList.processVersionHistory();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return postVersionList;
    }

    public static List<PostVersionList> readFromDirectory(Path dir) {
        return readFromDirectory(dir, true);
    }

    public static List<PostVersionList> readFromDirectory(Path dir, boolean processVersionHistory) {
        return Util.processFiles(dir,
                file -> fileNamePattern.matcher(file.toFile().getName()).matches(),
                file -> PostVersionList.readFromCSV(
                        dir,
                        Integer.parseInt(file.toFile().getName().replace(".csv", "")),
                        0, // cannot determine this from file name or file content
                        processVersionHistory
                )
        );
    }

    public void sort() {
        this.sort((v1, v2) ->
                v1.getPostHistoryId() < v2.getPostHistoryId() ? -1 : v1.getPostHistoryId() > v2.getPostHistoryId() ? 1 : 0
        );

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
    }

    public void processVersionHistory() {
        processVersionHistory(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public void processVersionHistory(Set<Integer> postBlockTypeFilter) {
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
    public void processVersionHistory(Config config, Set<Integer> postBlockTypeFilter) {
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
                    currentPostBlock.setRootPostBlockId(currentPostBlock.getId());
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
                Map<PostBlockVersion, List<PostBlockVersion>> matchedPredecessors = // matched predecessor -> list of successors
                        currentVersion.findMatchingPredecessors(
                            currentVersion.getPostBlocks(),
                            previousVersion.getPostBlocks(),
                            config,
                            postBlockTypeFilter
                        );

                // set predecessors of text and code blocks if only one predecessor matches and if this predecessor is
                // only matched by one block in the current version
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    currentPostBlock.setUniqueMatchingPred(matchedPredecessors);
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

                // finally, set the remaining predecessors using the order in the post (localId)
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    // predecessor for this post block not set yet and at least one matching predecessor found
                    if (currentPostBlock.getPred() == null && currentPostBlock.getMatchingPredecessors().size() > 0) {
                        currentPostBlock.setPredLocalId(matchedPredecessors);
                    }
                }

                // set root post block for all post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
                    if (currentPostBlock.getPred() == null) {
                        // block has no predecessor -> set itself as root post block
                        currentPostBlock.setRootPostBlock(currentPostBlock);
                        currentPostBlock.setRootPostBlockId(currentPostBlock.getId());
                    } else {
                        // block has predecessor -> set root post block of predecessor as root post block of this block
                        currentPostBlock.setRootPostBlock(currentPostBlock.getPred().getRootPostBlock());
                        currentPostBlock.setRootPostBlockId(currentPostBlock.getPred().getRootPostBlockId());
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

    public boolean setPostBlockPredContext(PostVersion currentVersion,
                                           PostVersion previousVersion,
                                           Set<Integer> postBlockTypeFilter,
                                           PostBlockVersion.MatchingStrategy matchingStrategy) {

        for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks(postBlockTypeFilter)) {
            boolean matched = currentPostBlock.setPredContext(currentVersion, previousVersion, matchingStrategy);
            if (matched) {
                return true;
            }
        }
        return false;
    }

    public PostBlockDiffList getDiffs() {
        return diffs;
    }

    public void insert(StatelessSession session) {
        for (PostVersion currentVersion : this) {
            // save current post version
            session.insert(currentVersion);
            // set post version for all post blocks and update them (pred, succ, similarity, root post block)
            for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                currentPostBlock.setPostVersionId(currentVersion.getId());
                session.update(currentPostBlock);
            }
        }

        diffs.insert(session);
    }

    // Needed for GT-App to display all links correctly with Commonmark
    public void normalizeLinks() {
        for (PostVersion postVersion : this) {
            String mergedTextBlocks = postVersion.getMergedTextBlockContent();
            List<Link> extractedLinks = Link.extractAll(mergedTextBlocks);

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

    public List<PostBlockLifeSpan> getPostBlockLifeSpans(Set<Integer> postBlockTypeFilter) {
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

    public Set<PostBlockConnection> getConnections(Set<Integer> postBlockTypeFilter) {
        Set<PostBlockConnection> connections = new HashSet<>();
        for (PostVersion currentVersion : this) {
            connections.addAll(currentVersion.getConnections(postBlockTypeFilter));
        }
        return connections;
    }

    public int getPossibleComparisons() {
        return getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleComparisons(Set<Integer> postBlockTypeFilter) {
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

    public int getPossibleConnections(Set<Integer> postBlockTypeFilter) {
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

    public int getPostBlockVersionCount(Set<Integer> postBlockTypeFilter) {
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

    public int getPostTypeId() {
        return postTypeId;
    }

    public int getFailedPredecessorComparisons() {
        return getFailedPredecessorComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getFailedPredecessorComparisons(Set<Integer> postBlockTypeFilter) {
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
