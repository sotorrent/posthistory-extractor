package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.diffs.PostBlockDiffList;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpan;
import de.unitrier.st.soposthistory.history.PostHistory;
import de.unitrier.st.soposthistory.urls.Link;
import de.unitrier.st.soposthistory.util.Config;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static de.unitrier.st.soposthistory.util.Util.processFiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostVersionList extends LinkedList<PostVersion> {
    //TODO: Add methods to extract history of code or text blocks (either ignoring versions where only blocks of the other type changed (global) or where one particular block did not change (local)

    public static final Pattern fileNamePattern = Pattern.compile("(\\d+)\\.csv");
    private static Logger logger = null;
    private static final CSVFormat csvFormatVersionList;

    private int postId;
    private boolean sorted;
    private PostBlockDiffList diffs;

    static {
        // configure logger
        try {
            logger = getClassLogger(PostVersionList.class, false);
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

    public PostVersionList(int postId) {
        super();
        this.postId = postId;
        this.sorted = false;
        this.diffs = new PostBlockDiffList();
    }

    public void reset() {
        this.sorted = false;
        this.diffs = new PostBlockDiffList();
        for (PostVersion currentVersion : this) {
            for (PostBlockVersion currentPostBlockVersion : currentVersion.getPostBlocks()) {
                currentPostBlockVersion.reset();
            }
            currentVersion.reset();
        }
    }

    public static PostVersionList readFromCSV(Path dir, int postId, int postTypeId) {
        return readFromCSV(dir, postId, postTypeId, true);
    }

    public static PostVersionList readFromCSV(Path dir, int postId, int postTypeId, boolean processVersionHistory) {
        // ensure that input directory exists
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + dir);
        }

        PostVersionList postVersionList = new PostVersionList(postId);
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
        return processFiles(dir,
                file -> fileNamePattern.matcher(file.toFile().getName()).matches(),
                file -> PostVersionList.readFromCSV(
                        dir,
                        Integer.parseInt(file.toFile().getName().replace(".csv", "")),
                        0 // cannot determine this from file name or file content
                )
        );
    }

    public void sort() {
        this.sort((v1, v2) ->
                v1.getPostHistoryId() < v2.getPostHistoryId() ? -1 : v1.getPostHistoryId() > v2.getPostHistoryId() ? 1 : 0
        );
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
     * @param postBlockTypeFilter Set of postBlockTypeIds (1 for text blocks, 2 for code blocks), mainly needed for evaluation of similarity metrics
     * @param config Configuration with similarity metrics and thresholds
     */
    public void processVersionHistory(Config config, Set<Integer> postBlockTypeFilter) {
        for (int i=0; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);

            if (config.getExtractUrls() && postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                currentVersion.extractUrlsFromTextBlocks();
            }

            int predIndex = i-1;
            int succIndex = i+1;

            if (predIndex == -1) {
                // current is first element
                currentVersion.setPred(null);
                currentVersion.setPredPostHistoryId(null);
                // the post blocks in the first version have themselves as root post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (!currentPostBlock.isSelected(postBlockTypeFilter)) {
                        continue;
                    }

                    currentPostBlock.setRootPostBlock(currentPostBlock);
                    currentPostBlock.setRootPostBlockId(currentPostBlock.getId());
                }
            } else {
                PostVersion previousVersion = this.get(predIndex);
                currentVersion.setPred(this.get(predIndex));
                currentVersion.setPredPostHistoryId(this.get(predIndex).getPostHistoryId());
                Map<PostBlockVersion, Integer> matchedPredecessors = new HashMap<>();

                // find matching predecessors by (1) equality of content and (2) similarity metric

                // find matching predecessors for text blocks
                if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                    matchedPredecessors.putAll(currentVersion.findMatchingPredecessors(
                            currentVersion.getTextBlocks(),
                            previousVersion.getTextBlocks(),
                            config
                    ));
                }

                // find matching predecessors for text blocks
                if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
                    matchedPredecessors.putAll(currentVersion.findMatchingPredecessors(
                            currentVersion.getCodeBlocks(),
                            previousVersion.getCodeBlocks(),
                            config
                    ));
                }

                // set predecessors of text and code blocks if only one predecessor matches and if this predecessor is only matched by one block in the current version
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (!currentPostBlock.isSelected(postBlockTypeFilter)) {
                        continue;
                    }

                    if (currentPostBlock.getMatchingPredecessors().size() == 1) {
                        // only one matching predecessor found
                        PostBlockVersion matchingPredecessor = currentPostBlock.getMatchingPredecessors().get(0);
                        if (matchedPredecessors.get(matchingPredecessor) == 1) {
                            // the matched predecessor is only matched for currentPostBlock
                            if (matchingPredecessor.isAvailable()) {
                                currentPostBlock.setPred(matchingPredecessor);
                                matchingPredecessor.setSucc(currentPostBlock);
                            }
                        }
                    }
                }

                // next, try to set remaining predecessors using context (neighboring blocks of post block and matching predecessor)
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (!currentPostBlock.isSelected(postBlockTypeFilter)) {
                        continue;
                    }

                    // predecessor for this post block not set yet and at least one matching predecessor found
                    if (currentPostBlock.getPred() == null && currentPostBlock.getMatchingPredecessors().size() > 0) {
                        currentPostBlock.setPredContext(currentVersion, previousVersion);
                    }
                }

                // finally, set the remaining predecessors using the order in the post (localId)
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (!currentPostBlock.isSelected(postBlockTypeFilter)) {
                        continue;
                    }

                    // predecessor for this post block not set yet and at least one matching predecessor found
                    if (currentPostBlock.getPred() == null && currentPostBlock.getMatchingPredecessors().size() > 0) {
                        currentPostBlock.setPredMinPos();
                    }
                }

                // set root post block for all post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (!currentPostBlock.isSelected(postBlockTypeFilter)) {
                        continue;
                    }

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
                currentVersion.setSuccPostHistoryId(null);
            } else {
                currentVersion.setSuccPostHistoryId(this.get(succIndex).getPostHistoryId());
            }
        }

        if (config.getComputeDiffs()) {
            // compute diffs
            diffs.fromPostVersionList(this);
        }
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

    // TODO: do we need this for the GT app or metrics comparison?
    public List<PostBlockLifeSpan> getPostBlockLifeSpans() {
        return getPostBlockLifeSpans(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    // TODO: do we need this for the GT app or metrics comparison?
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
                if (currentPostBlockVersion.isProcessed()) {
                    continue;
                }

                lifeSpans.add(PostBlockLifeSpan.fromPostBlockVersion(currentPostBlockVersion));
            }
        }

        // reset flag "processed"
        for (PostVersion currentPostVersion : this) {
            for (PostBlockVersion currentPostBlockVersion : currentPostVersion.getPostBlocks()) {
                currentPostBlockVersion.setProcessed(false);
            }
        }

        return lifeSpans;
    }

    public Set<PostBlockConnection> getConnections(Set<Integer> postBlockTypeFilter) {
        Set<PostBlockConnection> connections = new HashSet<>();
        for (PostVersion currentVersion : this) {
            connections.addAll(currentVersion.getConnections(postBlockTypeFilter));
        }
        return connections;
    }

    public int getPossibleConnections() {
        return getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleConnections(Set<Integer> postBlockTypeFilter) {
        // we cannot use PostVersion.getPossibleConnections() here, because the pred-References may not have been set yet
        int possibleConnections = 0;
        for (int i=1; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);
            PostVersion previousVersion = this.get(i-1);

            if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                int currentVersionTextBlocks = currentVersion.getTextBlocks().size();
                int previousVersionTextBlocks = previousVersion.getTextBlocks().size();
                possibleConnections += currentVersionTextBlocks * previousVersionTextBlocks;
            }

            if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
                int currentVersionCodeBlocks = currentVersion.getCodeBlocks().size();
                int previousVersionCodeBlocks = previousVersion.getCodeBlocks().size();
                possibleConnections += currentVersionCodeBlocks * previousVersionCodeBlocks;
            }
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
                .map(PostVersion::getPostBlocks)
                .flatMap(List::stream)
                .filter(b -> b.isSelected(postBlockTypeFilter))
                .count());
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
