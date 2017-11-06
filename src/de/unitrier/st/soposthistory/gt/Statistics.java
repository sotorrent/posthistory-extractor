package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

// TODO: move to metrics comparison project
public class Statistics {
    private static Logger logger = null;

    private static final Path rootPathToLargeSamples = Paths.get("testdata", "samples_10000");
    public static final List<Path> pathsToLargeSamplesFiles = getLargeSampleFilesPaths();
    public static final Path pathToMultipleConnectionsDir = Paths.get("testdata", "multiple_connections");
    public static final Path pathToMultipleConnectionsFile = Paths.get(pathToMultipleConnectionsDir.toString(), "multiple_possible_connections.csv");

    private static final CSVFormat csvFormatMultipleConnections;

    static {
        // configure logger
        try {
            logger = getClassLogger(Statistics.class, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format
        csvFormatMultipleConnections = CSVFormat.DEFAULT
                .withHeader("PostId", "PostHistoryId", "LocalId", "PostBlockTypeId", "PossiblePredecessorsCount", "PossibleSuccessorsCount", "PossiblePredecessorLocalIds", "PossibleSuccessorLocalIds")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\');
    }

    private static List<Path> getLargeSampleFilesPaths() {
        ArrayList<Path> pathsToLargeSamplesFiles = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            pathsToLargeSamplesFiles.add(Paths.get(rootPathToLargeSamples.toString(), "PostId_VersionCount_SO_17-06_sample_10000_" + i, "files"));
        }
        return pathsToLargeSamplesFiles;
    }

    public static void main(String[] args) {
        Statistics statistics = new Statistics();
        statistics.getMultiplePossibleConnections();
    }

    private void getMultiplePossibleConnections() {
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(
                pathToMultipleConnectionsFile.toFile()),
                csvFormatMultipleConnections
        )) {
            logger.info("Starting extraction of possible connections...");

            // header is automatically written
            for (Path currentSampleFiles : pathsToLargeSamplesFiles) {
                logger.info("Started with sample: " + currentSampleFiles);

                List<PostVersionList> postVersionLists = PostVersionList.readFromDirectory(currentSampleFiles);

                for (PostVersionList postVersionList : postVersionLists) {
                    for (int i = 0; i < postVersionList.size(); i++) {
                        PostVersion currentVersion = postVersionList.get(i);
                        PostVersion previousVersion = null;
                        if (i > 0) {
                            previousVersion = postVersionList.get(i - 1);
                        }
                        PostVersion nextVersion = null;
                        if (i < postVersionList.size() - 1) {
                            nextVersion = postVersionList.get(i + 1);
                        }

                        for (PostBlockVersion currentVersionPostBlock : currentVersion.getPostBlocks()) {
                            LinkedList<PostBlockVersion> possiblePredecessors = new LinkedList<>();
                            LinkedList<PostBlockVersion> possibleSuccessors = new LinkedList<>();

                            if (previousVersion != null) {
                                // get possible predecessors
                                for (PostBlockVersion previousVersionPostBlock : previousVersion.getPostBlocks()) {
                                    if (currentVersionPostBlock.getContent().equals(previousVersionPostBlock.getContent())) {
                                        possiblePredecessors.add(previousVersionPostBlock);
                                    }
                                }
                            }

                            if (nextVersion != null) {
                                // get possible successors
                                for (PostBlockVersion nextVersionPostBlock : nextVersion.getPostBlocks()) {
                                    if (currentVersionPostBlock.getContent().equals(nextVersionPostBlock.getContent())) {
                                        possibleSuccessors.add(nextVersionPostBlock);
                                    }
                                }
                            }

                            // write to CSV
                            if (possiblePredecessors.size() > 1 || possibleSuccessors.size() > 1) {
                                csvPrinter.printRecord(
                                        currentVersion.getPostId(),
                                        currentVersion.getPostHistoryId(),
                                        currentVersionPostBlock.getLocalId(),
                                        currentVersionPostBlock.getPostBlockTypeId(),
                                        possiblePredecessors.size(),
                                        possibleSuccessors.size(),
                                        Arrays.toString(possiblePredecessors.stream()
                                                    .map(PostBlockVersion::getLocalId)
                                                    .collect(Collectors.toList())
                                                    .toArray()),
                                        Arrays.toString(possibleSuccessors.stream()
                                                .map(PostBlockVersion::getLocalId)
                                                .collect(Collectors.toList())
                                                .toArray())
                                );
                            }
                        }
                    }
                }
                logger.info("Finished sample: " + currentSampleFiles);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: Lorik: Do we still need this?
//    private void copyPostsWithPossibleMultipleConnectionsIntoOwnDirectory() {
//        // get all postIds of file possible multiple connections.csv
//        List<Integer> postIdsOfAffectedPosts = new ArrayList<>();
//        CSVParser csvParser;
//        try {
//            csvParser = CSVParser.parse(
//                    pathToMultipleConnectionsFile.toFile(),
//                    StandardCharsets.UTF_8,
//                    MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
//            );
//
//            csvParser.getHeaderMap();
//            List<CSVRecord> records = null;
//
//            records = csvParser.getRecords();
//
//            for (CSVRecord record : records) {
//                postIdsOfAffectedPosts.add(Integer.valueOf(record.get("postId")));
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        for (Path pathToDirectory : pathsToLargeSamplesFiles) {
//            File file = pathToDirectory.toFile();
//            File[] allPostVersionListsInFolder = file.listFiles((dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java
//
//            assert allPostVersionListsInFolder != null;
//            for (File postVersionListFile : allPostVersionListsInFolder) {
//                Integer tmpPostId = Integer.valueOf(postVersionListFile.getName().replace(".csv", ""));
//                if (postIdsOfAffectedPosts.contains(tmpPostId)) {
//                    try {
//                        Files.copy(
//                                postVersionListFile.toPath(),
//                                Paths.get(pathToDirectoryToCopy.toString(), tmpPostId + ".csv"));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
}
