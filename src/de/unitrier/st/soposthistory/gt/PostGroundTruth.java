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
import java.nio.file.Files;
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
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + dir);
        }

        Path pathToCSVFile = Paths.get(dir.toString(), "completed_" + postId + ".csv");
        PostGroundTruth gt = new PostGroundTruth(postId);

        try (CSVParser csvParser = new CSVParser(new FileReader(pathToCSVFile.toFile()), csvFormatGT)) {
            logger.info("Reading GT from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postIdFile = Integer.parseInt(currentRecord.get("PostId"));
                if (postIdFile != postId) {
                    throw new IllegalArgumentException("Wrong post id in GT: " + postIdFile + " instead of " + postId);
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
                    if (m.find()) {
                        return PostGroundTruth.readFromCSV(
                                dir,
                                Integer.parseInt(m.group(1))
                        );
                    } else {
                        throw new IllegalArgumentException("Invalid file name for a ground truth CSV.");
                    }
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

    // TODO: do we need this for the GT app or metrics comparison?
    public List<PostBlockLifeSpan> getPostBlockLifeSpans() {
        return getPostBlockLifeSpans(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    // TODO: do we need this for the GT app or metrics comparison?
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

                    // this version does not have a predecessor
                    if (currentLifeSpanVersion.getSuccLocalId() == null) {
                        break;
                    }

                    List<PostBlockLifeSpanVersion> nextVersion = versions.get(postHistoryIds.get(i+versionCount+1));

                    final int succLocalId = currentLifeSpanVersion.getSuccLocalId();
                    List<PostBlockLifeSpanVersion> nextLifeSpanVersionCandidates = nextVersion.stream()
                            .filter(v -> v.getLocalId() == succLocalId)
                            .collect(Collectors.toList());

                    if (nextLifeSpanVersionCandidates.size() == 0) {
                        throw new IllegalStateException("No successor found.");
                    }

                    if (nextLifeSpanVersionCandidates.size() > 1) {
                        throw new IllegalStateException("More than one successor found.");
                    }

                    PostBlockLifeSpanVersion nextLifeSpanVersion = nextLifeSpanVersionCandidates.get(0);

                    if (!currentLifeSpanVersion.getSuccLocalId().equals(nextLifeSpanVersion.getLocalId()) ||
                            !nextLifeSpanVersion.getPredLocalId().equals(currentLifeSpanVersion.getLocalId())) {
                        throw new IllegalStateException("Predecessor and Successor LocalIds do not match.");
                    }

                    currentLifeSpanVersion = nextLifeSpanVersion;
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

        if ((postBlockTypeFilter.equals(PostBlockVersion.getAllPostBlockTypeIdFilters()) && lifeSpanVersionCount != this.size())
                || (postBlockTypeFilter.equals(TextBlockVersion.getPostBlockTypeIdFilter()) && lifeSpanVersionCount != this.getTextBlocks().size())
                || (postBlockTypeFilter.equals(CodeBlockVersion.getPostBlockTypeIdFilter()) && lifeSpanVersionCount != this.getCodeBlocks().size())) {
            throw new IllegalStateException("The number of lifespan versions differs from the number of versions in the ground truth.");
        }

        return postBlockLifeSpans;
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
                if (currentLifeSpanVersion.isSelected(postBlockTypeFilter)) {
                    // first element in a PostBlockLifeSpan -> no connections
                    if (currentLifeSpanVersion.getPredLocalId() != null) {
                        // search for matching lifespan version(s) in previous post version
                        final int predLocalId = currentLifeSpanVersion.getPredLocalId();
                        final int postBlockTypeId = currentLifeSpanVersion.getPostBlockTypeId();
                        List<PostBlockLifeSpanVersion> predLifeSpanVersionCandidates = previousVersion.stream()
                                .filter(v -> v.getLocalId() == predLocalId && v.getPostBlockTypeId() == postBlockTypeId)
                                .collect(Collectors.toList());

                        if (predLifeSpanVersionCandidates.size() == 0) {
                            throw new IllegalStateException("No predecessor found.");
                        }

                        if (predLifeSpanVersionCandidates.size() > 1) {
                            throw new IllegalStateException("More than one successor found.");
                        }

                        PostBlockLifeSpanVersion predLifeSpanVersion = predLifeSpanVersionCandidates.get(0);

                        if (!currentLifeSpanVersion.getPredLocalId().equals(predLifeSpanVersion.getLocalId()) ||
                                !predLifeSpanVersion.getSuccLocalId().equals(currentLifeSpanVersion.getLocalId())) {
                            throw new IllegalStateException("Predecessor and Successor LocalIds do not match.");
                        }

                        connections.add(new PostBlockConnection(predLifeSpanVersion, currentLifeSpanVersion));
                    }
                }
            }
        }

        return connections;
    }

    public int getPossibleConnections() {
        return getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    private int getPossibleConnections(Set<Integer> postBlockTypeFilter) {
        int possibleConnections = 0;
        for (int postHistoryId : postHistoryIds) {
            possibleConnections += getPossibleConnections(postHistoryId, postBlockTypeFilter);
        }
        return possibleConnections;
    }

    public int getPossibleConnections(int postHistoryId) {
        return getPossibleConnections(postHistoryId, PostBlockVersion.getAllPostBlockTypeIdFilters());
    }

    public int getPossibleConnections(int postHistoryId, Set<Integer> postBlockTypeFilter) {
        int possibleConnections = 0;
        int index = postHistoryIds.indexOf(postHistoryId);

        if (index >= 1) {
            // first version cannot have connections
            List<PostBlockLifeSpanVersion> currentVersion = versions.get(postHistoryIds.get(index));
            List<PostBlockLifeSpanVersion> previousVersion = versions.get(postHistoryIds.get(index-1));

            if (postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId)) {
                int currentVersionTextBlocks = Math.toIntExact(currentVersion.stream()
                        .filter(b -> b.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                        .count());
                int previousVersionTextBlocks = Math.toIntExact(previousVersion.stream()
                        .filter(b -> b.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                        .count());
                possibleConnections += currentVersionTextBlocks * previousVersionTextBlocks;
            }

            if (postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId)) {
                int currentVersionCodeBlocks = Math.toIntExact(currentVersion.stream()
                        .filter(b -> b.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                        .count());
                int previousVersionCodeBlocks = Math.toIntExact(previousVersion.stream()
                        .filter(b -> b.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                        .count());
                possibleConnections += currentVersionCodeBlocks * previousVersionCodeBlocks;
            }
        }

        return possibleConnections;
    }

    public List<Integer> getPostHistoryIds() {
        return postHistoryIds;
    }

    public int getPostId() {
        return postId;
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
