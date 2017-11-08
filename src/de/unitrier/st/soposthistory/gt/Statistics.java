package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.util.Util;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// TODO: move to metrics comparison project
public class Statistics {
    private static Logger logger = null;

    private static final Path rootPathToGTSamples = Paths.get("testdata", "samples_gt");
    public static final List<Path> pathsToGTSamples = getGTSamples(rootPathToGTSamples);

    private static final Path rootPathToTestSamples = Paths.get("testdata", "samples_test");
    public static final List<Path> pathsToTestSamples = getTestSamples(rootPathToTestSamples);

    public static final Path pathToMultipleConnectionsDir = Paths.get("testdata", "multiple_connections");
    public static final Path pathToMultipleConnectionsFile = Paths.get(pathToMultipleConnectionsDir.toString(), "multiple_possible_connections.csv");
    private static final Path pathToMultipleConnectionsPostsFile = Paths.get(pathToMultipleConnectionsDir.toString(), "multiple_possible_connections_posts.csv");

    private static final Path outputDir = Paths.get("output");

    public static final CSVFormat csvFormatMultipleConnections;

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

    public static void main(String[] args) {
        Statistics statistics = new Statistics();
        //statistics.getMultiplePossibleConnections();
        statistics.copyPostsWithPossibleMultipleConnectionsIntoDirectory();
    }

    private static List<Path> getGTSamples(Path rootPathToGTSamples) {
        ArrayList<Path> pathsToGTSamples = new ArrayList<>(6);
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_1"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_2"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_1+"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_2+"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_Java_17-06_sample_100_1"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_Java_17-06_sample_100_2"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17_06_sample_unclear_matching"));
        pathsToGTSamples.add(Paths.get(rootPathToGTSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_multiple_possible_links"));
        return pathsToGTSamples;
    }

    private static List<Path> getTestSamples(Path rootPathToTestSamples) {
        ArrayList<Path> pathsToTestSamples = new ArrayList<>(12);
        for (int i = 1; i <= 10; i++) {
            // test data
            pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_10000_" + i));
        }
        // sample with many versions (n=100)
        pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_100_most_versions"));
        // sample with multiple possible connections (n=498)
        pathsToTestSamples.add(Paths.get(rootPathToTestSamples.toString(), "PostId_VersionCount_SO_17-06_sample_multiple_possible_links"));
        return pathsToTestSamples;
    }

    private void getMultiplePossibleConnections() {

        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(
                pathToMultipleConnectionsFile.toFile()),
                csvFormatMultipleConnections);
             CSVPrinter csvPrinterPosts = new CSVPrinter(new FileWriter(
                     pathToMultipleConnectionsPostsFile.toFile()),
                     MetricComparisonManager.csvFormatPostIds
             )) {

            logger.info("Starting extraction of possible connections...");

            Set<PostVersionList> selectedPostVersionLists = new HashSet<>();

            // header is automatically written
            for (Path currentSample : pathsToTestSamples.subList(0, 10)) { // only consider large samples here
                logger.info("Processing sample: " + currentSample);

                Path postVersionListDir = Paths.get(currentSample.toString(), "files");
                List<PostVersionList> postVersionLists = PostVersionList.readFromDirectory(postVersionListDir);

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

                            // write version data to CSV file
                            if (possiblePredecessors.size() > 1 || possibleSuccessors.size() > 1) {
                                selectedPostVersionLists.add(postVersionList);

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
                logger.info("Processed sample: " + currentSample);
            }

            logger.info("Writing list with selected posts to CSV file: " + pathToMultipleConnectionsPostsFile.toFile().getName());

            for (PostVersionList postVersionList : selectedPostVersionLists) {
                csvPrinterPosts.printRecord(
                        postVersionList.getPostId(),
                        postVersionList.getPostTypeId(),
                        postVersionList.size()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method has been used to create the sample "PostId_VersionCount_SO_17-06_sample_100_multiple_possible_links",
    // which contains 100 randomly selected posts with multiple possible connections together with a manually created
    // ground truth
    private void copyPostsWithPossibleMultipleConnectionsIntoDirectory() {
        // get all postIds from file multiple_possible_connections.csv
        Set<Integer> postIds = new HashSet<>();
        try (CSVParser csvParser = CSVParser.parse(
                pathToMultipleConnectionsFile.toFile(),
                StandardCharsets.UTF_8,
                MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
            )) {

            Util.ensureEmptyDirectoryExists(outputDir);

            for (CSVRecord record : csvParser) {
                postIds.add(Integer.valueOf(record.get("PostId")));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Path pathToSample : pathsToTestSamples.subList(0, 10)) { // only consider large samples here

            File file = Paths.get(pathToSample.toString(), "files").toFile();
            File[] postVersionListFilesInFolder = file.listFiles(
                    (dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())
            );

            assertNotNull(postVersionListFilesInFolder);

            for (File postVersionListFile : postVersionListFilesInFolder) {
                Matcher matcher = PostVersionList.fileNamePattern.matcher(postVersionListFile.getName());
                if (matcher.find()) {
                    int postId = Integer.parseInt(matcher.group(1));
                    if (postIds.contains(postId)) {
                        try {
                            Files.copy(
                                    postVersionListFile.toPath(),
                                    Paths.get(outputDir.toString(), postId + ".csv")
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
