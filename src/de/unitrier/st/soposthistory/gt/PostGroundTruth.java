package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostGroundTruth extends LinkedList<PostBlockLifeSpanVersion> {

    public static final Pattern fileNamePattern = Pattern.compile("completed_(\\d+)\\.csv");
    private static Logger logger = null;
    private static final CSVFormat csvFormatGT;

    private int postId;
    private List<Integer> postHistoryIds;
    private Map<Integer, List<PostBlockLifeSpanVersion>> versions; // postHistoryId -> PostBlockLifeSpanVersions

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(PostGroundTruth.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for ground truth
        csvFormatGT = CSVFormat.DEFAULT
                .withHeader("PostId", "PostHistoryId", "PostBlockTypeId", "LocalId", "PredLocalId", "SuccLocalId", "Comment")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null")
                .withFirstRecordAsHeader();
    }

    PostGroundTruth(int postId) {
        this.postId = postId;
        this.versions = null;
    }

    public static PostGroundTruth readFromCSV(Path dir, int postId) {
        // ensure that input directory exists
        Util.ensureDirectoryExists(dir);

        Path pathToCSVFile = Paths.get(dir.toString(), "completed_" + postId + ".csv");
        PostGroundTruth gt = new PostGroundTruth(postId);

        try (CSVParser csvParser = new CSVParser(new FileReader(pathToCSVFile.toFile()), csvFormatGT)) {
            logger.info("Reading GT from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postIdFile = Integer.parseInt(currentRecord.get("PostId"));
                if (postIdFile != postId) {
                    String msg = "Wrong post id in GT: " + postIdFile + " instead of " + postId;
                    logger.warning(msg);
                    throw new IllegalArgumentException(msg);
                }
                int postHistoryId = Integer.parseInt(currentRecord.get("PostHistoryId"));
                int postBlockTypeId = Integer.parseInt(currentRecord.get("PostBlockTypeId"));
                int localId = Integer.parseInt(currentRecord.get("LocalId"));
                Integer predLocalId = currentRecord.get("PredLocalId") == null ? null : Integer.parseInt(currentRecord.get("PredLocalId"));
                Integer succLocalId = currentRecord.get("SuccLocalId") == null ? null : Integer.parseInt(currentRecord.get("SuccLocalId"));
                String comment = currentRecord.get("Comment");

                gt.add(new PostBlockLifeSpanVersion(postId, postHistoryId, postBlockTypeId, localId,
                        predLocalId, succLocalId, comment));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        gt.sort();
        gt.orderByVersions();

        return gt;
    }

    public static List<PostGroundTruth> readFromDirectory(Path dir) {
        return Util.processFiles(dir,
                file -> fileNamePattern.matcher(file.toFile().getName()).matches(),
                file -> {
                    Matcher m = fileNamePattern.matcher(file.toFile().getName());
                    if (!m.find()) {
                        String msg = "Invalid file name for a ground truth CSV: " + dir;
                        logger.warning(msg);
                        throw new IllegalArgumentException(msg);
                    }
                    return PostGroundTruth.readFromCSV(
                            dir,
                            Integer.parseInt(m.group(1))
                    );
                }
        );
    }

    private void sort() {
        this.sort((gtBlock1, gtBlock2) -> {
            int result = Integer.compare(gtBlock1.getPostHistoryId(), gtBlock2.getPostHistoryId());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(gtBlock1.getLocalId(), gtBlock2.getLocalId());
            }
        });
    }

    private void orderByVersions() {
        versions = new HashMap<>();

        Map<Integer, List<PostBlockLifeSpanVersion>> versionsPerPostHistoryId = this.stream()
                .collect(Collectors.groupingBy(PostBlockLifeSpanVersion::getPostHistoryId));

        postHistoryIds = new LinkedList<>(versionsPerPostHistoryId.keySet());
        postHistoryIds.sort(Integer::compare);

        for (int postHistoryId : postHistoryIds) {
            List<PostBlockLifeSpanVersion> versions = versionsPerPostHistoryId.get(postHistoryId);
            this.versions.put(postHistoryId, versions);
        }
    }

    public List<PostBlockLifeSpan> getPostBlockLifeSpans() {
        return getPostBlockLifeSpans(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public List<PostBlockLifeSpan> getPostBlockLifeSpans(Set<Integer> postBlockTypeFilter) {
        List<PostBlockLifeSpan> postBlockLifeSpans = new LinkedList<>();

        for (int i = 0; i< postHistoryIds.size(); i++) {
            List<PostBlockLifeSpanVersion> currentVersion = versions.get(postHistoryIds.get(i));

            for (PostBlockLifeSpanVersion currentLifeSpanVersion : currentVersion) {
                // apply filter
                if (!currentLifeSpanVersion.isSelected(postBlockTypeFilter)) {
                    continue;
                }

                PostBlockLifeSpan lifeSpan = new PostBlockLifeSpan(
                        currentLifeSpanVersion.getPostId(),
                        currentLifeSpanVersion.getPostBlockTypeId()
                );

                // find successor of this lifespan version
                int versionCount = 0;
                while (i+versionCount < postHistoryIds.size()) {
                    // skip life span versions that have already been processed
                    if (currentLifeSpanVersion.isProcessed()) {
                        break;
                    }

                    currentLifeSpanVersion.setProcessed(true);
                    lifeSpan.add(currentLifeSpanVersion);

                    // this version does not have a successor
                    if (currentLifeSpanVersion.getSuccLocalId() == null) {
                        break;
                    }

                    // retrieve successor from candidates
                    List<PostBlockLifeSpanVersion> nextVersion = versions.get(postHistoryIds.get(i+versionCount+1));
                    currentLifeSpanVersion = retrieveSuccessor(currentLifeSpanVersion, nextVersion);

                    versionCount++;
                }

                if (lifeSpan.size() > 0) {
                    postBlockLifeSpans.add(lifeSpan);
                }
            }
        }

        int lifeSpanVersionCount = postBlockLifeSpans.stream()
                .map(List::size)
                .mapToInt(Integer::intValue)
                .sum();

        // validate number of lifespan versions
        String msg = "The number of lifespan versions differs from the number of versions in the ground truth "
                + "(expected: " + lifeSpanVersionCount + "; actual: ";
        if (postBlockTypeFilter.equals(PostBlockVersion.getAllPostBlockTypeIdFilters()) && lifeSpanVersionCount != this.size()) {
            msg += this.size() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        } else if (postBlockTypeFilter.equals(TextBlockVersion.getPostBlockTypeIdFilter()) && lifeSpanVersionCount != this.getTextBlocks().size()) {
            msg +=  + this.getTextBlocks().size() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        } else if (postBlockTypeFilter.equals(CodeBlockVersion.getPostBlockTypeIdFilter()) && lifeSpanVersionCount != this.getCodeBlocks().size()) {
            msg +=  + this.getCodeBlocks().size() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        return postBlockLifeSpans;
    }

    private PostBlockLifeSpanVersion retrieveSuccessor(
            PostBlockLifeSpanVersion currentLifeSpanVersion,
            List<PostBlockLifeSpanVersion> successorCandidates) {

        final int succLocalId = currentLifeSpanVersion.getSuccLocalId();
        List<PostBlockLifeSpanVersion> selectedSuccessorCandidates = successorCandidates.stream()
                .filter(v -> v.getLocalId() == succLocalId)
                .collect(Collectors.toList());

        if (selectedSuccessorCandidates.size() == 0) {
            String msg = "No successor found for " + currentLifeSpanVersion;
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (selectedSuccessorCandidates.size() > 1) {
            String msg = "More than one successor found for " + currentLifeSpanVersion;
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        PostBlockLifeSpanVersion successor = selectedSuccessorCandidates.get(0);

        if (!successor.getPredLocalId().equals(currentLifeSpanVersion.getLocalId())) {
            String msg = "Predecessor LocalIds do not match for postId " + postId
                    + ", postHistoryId (" + currentLifeSpanVersion.getPostHistoryId()
                    + ", " + successor.getPostHistoryId() + ") "
                    + "(expected: " + currentLifeSpanVersion.getLocalId() + "; actual: "
                    + successor.getPredLocalId() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (!currentLifeSpanVersion.getSuccLocalId().equals(successor.getLocalId())) {
            String msg = "Successor LocalIds do not match " + postId
                    + ", postHistoryId (" + currentLifeSpanVersion.getPostHistoryId()
                    + ", " + successor.getPostHistoryId() + ") "
                    + "(expected: " + currentLifeSpanVersion.getSuccLocalId() + "; actual: "
                    + successor.getLocalId() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        return successor;
    }

    private PostBlockLifeSpanVersion retrievePredecessor(
            PostBlockLifeSpanVersion currentLifeSpanVersion,
            List<PostBlockLifeSpanVersion> predecessorCandidates) {

        final int predLocalId = currentLifeSpanVersion.getPredLocalId();
        final int postBlockTypeId = currentLifeSpanVersion.getPostBlockTypeId();
        List<PostBlockLifeSpanVersion> selectedPredecessorCandidates = predecessorCandidates.stream()
                .filter(v -> v.getLocalId() == predLocalId && v.getPostBlockTypeId() == postBlockTypeId)
                .collect(Collectors.toList());


        if (selectedPredecessorCandidates.size() == 0) {
            String msg = "No predecessor found for " + currentLifeSpanVersion;
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (selectedPredecessorCandidates.size() > 1) {
            String msg = "More than one predecessor found for " + currentLifeSpanVersion;
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        PostBlockLifeSpanVersion predecessor = selectedPredecessorCandidates.get(0);

        if (!currentLifeSpanVersion.getPredLocalId().equals(predecessor.getLocalId())) {
            String msg = "Predecessor LocalIds do not match"
                    + "(expected: " + currentLifeSpanVersion.getPredLocalId() + "; actual: "
                    + predecessor.getLocalId() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        if (!predecessor.getSuccLocalId().equals(currentLifeSpanVersion.getLocalId())) {
            String msg = "Successor LocalIds do not match"
                    + "(expected: " + currentLifeSpanVersion.getLocalId() + "; actual: "
                    + predecessor.getSuccLocalId() + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        }

        return predecessor;
    }

    public List<PostBlockLifeSpanVersion> getTextBlocks() {
        return this.stream()
                .filter(v -> v.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                .collect(Collectors.toList());
    }

    public List<PostBlockLifeSpanVersion> getCodeBlocks() {
        return this.stream()
                .filter(v -> v.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                .collect(Collectors.toList());
    }

    public List<PostBlockLifeSpanVersion> getPostVersion(int postHistoryId) {
        return versions.get(postHistoryId);
    }

    public Set<PostBlockConnection> getConnections() {
        return getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public Set<PostBlockConnection> getConnections(Set<Integer> postBlockTypeFilter) {
        Set<PostBlockConnection> connections = new HashSet<>();
        for (int postHistoryId : postHistoryIds) {
            connections.addAll(getConnections(postHistoryId, postBlockTypeFilter));
        }
        return connections;
    }

    public Set<PostBlockConnection> getConnections(int postHistoryId) {
        return getConnections(postHistoryId, PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public Set<PostBlockConnection> getConnections(int postHistoryId, Set<Integer> postBlockTypeFilter) {
        Set<PostBlockConnection> connections = new HashSet<>();
        int index = postHistoryIds.indexOf(postHistoryId);

        if (index >= 1) {
            // first version cannot have connections
            List<PostBlockLifeSpanVersion> currentVersion = getPostVersion(postHistoryId);
            List<PostBlockLifeSpanVersion> previousVersion = getPostVersion(postHistoryIds.get(index-1));

            for (PostBlockLifeSpanVersion currentLifeSpanVersion : currentVersion) {
                // apply filter
                if (!currentLifeSpanVersion.isSelected(postBlockTypeFilter)) {
                    continue;
                }

                // first element in a PostBlockLifeSpan -> no connections
                if (currentLifeSpanVersion.getPredLocalId() == null) {
                    continue;
                }

                // search for matching lifespan version(s) in previous post version
                PostBlockLifeSpanVersion predecessor = retrievePredecessor(currentLifeSpanVersion, previousVersion);

                // add connections
                connections.add(new PostBlockConnection(predecessor, currentLifeSpanVersion));
            }
        }

        return connections;
    }

    public int getPossibleComparisons() {
        return getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleComparisons(Set<Integer> postBlockTypeFilter) {
        int possibleComparisons = 0;
        for (int postHistoryId : postHistoryIds) {
            possibleComparisons += getPossibleComparisons(postHistoryId, postBlockTypeFilter);
        }
        return possibleComparisons;
    }

    public int getPossibleComparisons(int postHistoryId) {
        return getPossibleComparisons(postHistoryId, PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleComparisons(int postHistoryId, Set<Integer> postBlockTypeFilter) {
        int index = postHistoryIds.indexOf(postHistoryId);

        // first version cannot have comparisons
        if (index < 1) {
            return 0;
        }

        // determine possible comparisons
        int possibleComparisons = 0;
        List<PostBlockLifeSpanVersion> currentVersion = versions.get(postHistoryIds.get(index));
        List<PostBlockLifeSpanVersion> previousVersion = versions.get(postHistoryIds.get(index-1));

        // text blocks
        if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
            int currentVersionTextBlocks = Math.toIntExact(currentVersion.stream()
                    .filter(b -> b.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                    .count());
            int previousVersionTextBlocks = Math.toIntExact(previousVersion.stream()
                    .filter(b -> b.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                    .count());
            possibleComparisons += currentVersionTextBlocks * previousVersionTextBlocks;
        }

        // code blocks
        if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
            int currentVersionCodeBlocks = Math.toIntExact(currentVersion.stream()
                    .filter(b -> b.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                    .count());
            int previousVersionCodeBlocks = Math.toIntExact(previousVersion.stream()
                    .filter(b -> b.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                    .count());
            possibleComparisons += currentVersionCodeBlocks * previousVersionCodeBlocks;
        }

        return possibleComparisons;
    }

    public List<Integer> getPostHistoryIds() {
        return postHistoryIds;
    }

    public int getPostId() {
        return postId;
    }

    public Map<Integer, List<PostBlockLifeSpanVersion>> getVersions() {
        return versions;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("PostGroundTruth for PostId " + postId + ":\n");
        for (PostBlockLifeSpanVersion version : this) {
            result.append(version);
            result.append("\n");
        }
        return result.toString();
    }

}
