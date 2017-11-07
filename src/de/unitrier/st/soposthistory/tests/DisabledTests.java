package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.gt.*;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
class DisabledTests {
    private static Logger logger;

    private static Path pathToOldMetricComparisons = Paths.get(
            "testdata", "metrics_comparison", "results_metric_comparison_old.csv"
    );

    static {
        try {
            logger = getClassLogger(DisabledTests.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCompareMetricComparisonManagerWithComparisonFromOldProject() {
        MetricComparisonManager manager = MetricComparisonManager.create(
                "TestManager", BlockLifeSpanAndGroundTruthTest.pathToPostIdList,
                BlockLifeSpanAndGroundTruthTest.pathToPostHistory, BlockLifeSpanAndGroundTruthTest.pathToGroundTruth,
                true
        );

        List<Integer> postHistoryIds_3758880 = manager.getPostVersionLists().get(3758880).getPostHistoryIds();
        List<Integer> postHistoryIds_22037280 = manager.getPostVersionLists().get(22037280).getPostHistoryIds();

        manager.compareMetrics();
        manager.writeToCSV(BlockLifeSpanAndGroundTruthTest.outputDir);

        CSVParser csvParser;

        try {
            csvParser = CSVParser.parse(
                    pathToOldMetricComparisons.toFile(),
                    StandardCharsets.UTF_8,
                    MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
            );

            csvParser.getHeaderMap();
            List<CSVRecord> records = csvParser.getRecords();
            for (CSVRecord record : records) {
                String metric = record.get("metric");

                Double threshold = Double.valueOf(record.get("threshold"));
                // comparison manager computes only thresholds mod 0.10 by now so unequal thresholds will be skipped
                if ((int) (threshold * 100) % 10 != 0) {
                    continue;
                }

                Integer postId = Integer.valueOf(record.get("postid"));

                Integer postHistoryId = null;

                Integer truePositivesText = null;
                Integer trueNegativesText = null;
                Integer falsePositivesText = null;
                Integer falseNegativesText = null;

                Integer truePositivesCode = null;
                Integer trueNegativesCode = null;
                Integer falsePositivesCode = null;
                Integer falseNegativesCode = null;

                try {
                    postHistoryId = Integer.valueOf(record.get("posthistoryid"));

                    truePositivesText = Integer.valueOf(record.get("#truepositivestext"));
                    trueNegativesText = Integer.valueOf(record.get("#truenegativestext"));
                    falsePositivesText = Integer.valueOf(record.get("#falsepositivestext"));
                    falseNegativesText = Integer.valueOf(record.get("#falsenegativestext"));

                    truePositivesCode = Integer.valueOf(record.get("#truepositivescode"));
                    trueNegativesCode = Integer.valueOf(record.get("#truenegativescode"));
                    falsePositivesCode = Integer.valueOf(record.get("#falsepositivescode"));
                    falseNegativesCode = Integer.valueOf(record.get("#falsenegativescode"));
                } catch (NumberFormatException ignored) {
                }

                MetricComparison tmpMetricComparison = manager.getMetricComparison(postId, metric, threshold);

                if (postHistoryId == null) {
                    List<Integer> postHistoryIds = null;
                    if (postId == 3758880) {
                        postHistoryIds = postHistoryIds_3758880;
                    } else if (postId == 22037280) {
                        postHistoryIds = postHistoryIds_22037280;
                    }

                    assertNotNull(postHistoryIds);
                    for (Integer tmpPostHistoryId : postHistoryIds) {
                        MetricComparison.MetricResult resultsText = tmpMetricComparison.getResultsText(tmpPostHistoryId);
                        assertNull(resultsText.getTruePositives());
                        assertNull(resultsText.getFalsePositives());
                        assertNull(resultsText.getTrueNegatives());
                        assertNull(resultsText.getFalseNegatives());

                        MetricComparison.MetricResult resultsCode = tmpMetricComparison.getResultsCode(tmpPostHistoryId);
                        assertNull(resultsCode.getTruePositives());
                        assertNull(resultsCode.getFalsePositives());
                        assertNull(resultsCode.getTrueNegatives());
                        assertNull(resultsCode.getFalseNegatives());

                    }
                } else {
                    // TODO: Check true negatives for text and code
                    MetricComparison.MetricResult resultsText = tmpMetricComparison.getResultsText(postHistoryId);
                    assertEquals(truePositivesText, resultsText.getTruePositives());
                    assertEquals(falsePositivesText, resultsText.getFalsePositives());
                    assertEquals(falseNegativesText, resultsText.getFalseNegatives());

                    MetricComparison.MetricResult resultsCode = tmpMetricComparison.getResultsCode(postHistoryId);
                    assertEquals(truePositivesCode, resultsCode.getTruePositives());
                    assertEquals(falsePositivesCode, resultsCode.getFalsePositives());
                    assertEquals(falseNegativesCode, resultsCode.getFalseNegatives());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSmallSamplesParsable() {
        testSamples(Statistics.pathsToSmallSamplesFiles);
    }

    @Test
    void testLargeSamplesParsable() {
        testSamples(Statistics.pathsToLargeSamplesFiles);
    }

    private void testSamples(List<Path> paths) {
        for (Path currentPath : paths) {
            File[] postHistoryFiles = currentPath.toFile().listFiles(
                    (dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())
            );

            assertNotNull(postHistoryFiles);

            for (File postHistoryFile : postHistoryFiles) {
                Matcher fileNameMatcher = PostVersionList.fileNamePattern.matcher(postHistoryFile.getName());
                if (fileNameMatcher.find()) {
                    int postId = Integer.parseInt(fileNameMatcher.group(1));
                    // no exception should be thrown for the following two lines
                    PostVersionList postVersionList = PostVersionList.readFromCSV(currentPath, postId, -1);
                    postVersionList.normalizeLinks();
                    assertTrue(postVersionList.size() > 0);
                }
            }
        }
    }

    @Test
    void comparePossibleMultipleConnectionsWithOldComparisonProject() {
        // This test case "fails" because the extraction of post blocks has been changed since the creation of the old file.

        File oldFile = Paths.get(Statistics.pathToMultipleConnectionsDir.toString(),
                "multiple_possible_connections_old.csv").toFile();
        File newFile = Statistics.pathToMultipleConnectionsFile.toFile();

        CSVParser csvParserOld, csvParserNew;
        try {
            // parse old records
            csvParserOld = CSVParser.parse(
                    oldFile,
                    StandardCharsets.UTF_8,
                    Statistics.csvFormatMultipleConnections.withFirstRecordAsHeader()
                        .withHeader("postId", "postHistoryId", "localId", "blockTypeId",
                                "possiblePredOrSuccLocalIds", "numberOfPossibleSuccessorsOrPredecessors")
            );
            List<CSVRecord> oldRecords = csvParserOld.getRecords();
            List<MultipleConnectionsResultOld> oldResults = new ArrayList<>(oldRecords.size());

            for (CSVRecord record : oldRecords) {
                int postId = Integer.parseInt(record.get("postId"));
                int postHistoryId = Integer.parseInt(record.get("postHistoryId"));
                int localId = Integer.parseInt(record.get("localId"));
                int postBlockTypeId = Integer.parseInt(record.get("blockTypeId"));
                String possiblePredOrSuccLocalIds = record.get("possiblePredOrSuccLocalIds");
                int numberOfPossibleSuccessorsOrPredecessors = Integer.parseInt(record.get("numberOfPossibleSuccessorsOrPredecessors"));

                oldResults.add(new MultipleConnectionsResultOld(postId, postHistoryId, localId, postBlockTypeId,
                        possiblePredOrSuccLocalIds, numberOfPossibleSuccessorsOrPredecessors));
            }

            // parse new records
            csvParserNew = CSVParser.parse(
                    newFile,
                    StandardCharsets.UTF_8,
                    Statistics.csvFormatMultipleConnections.withFirstRecordAsHeader()
            );

            List<CSVRecord> newRecords = csvParserNew.getRecords();
            List<MultipleConnectionsResultNew> newResults = new ArrayList<>(newRecords.size());

            for (CSVRecord record : newRecords) {
                int postId = Integer.parseInt(record.get("PostId"));
                int postHistoryId = Integer.parseInt(record.get("PostHistoryId"));
                int localId = Integer.parseInt(record.get("LocalId"));
                int postBlockTypeId = Integer.parseInt(record.get("PostBlockTypeId"));
                int possiblePredecessorsCount = Integer.parseInt(record.get("PossiblePredecessorsCount"));
                int possibleSuccessorsCount = Integer.parseInt(record.get("PossibleSuccessorsCount"));
                String possiblePredecessorLocalIds = record.get("PossiblePredecessorLocalIds");
                String possibleSuccessorLocalIds = record.get("PossibleSuccessorLocalIds");

                newResults.add(new MultipleConnectionsResultNew(postId, postHistoryId, localId, postBlockTypeId,
                        possiblePredecessorsCount, possibleSuccessorsCount,
                        possiblePredecessorLocalIds, possibleSuccessorLocalIds));
            }

            // compare old and new results
            for (MultipleConnectionsResultNew multipleConnectionsResultNew : newResults) {
                int newPostId = multipleConnectionsResultNew.postId;
                int newPostHistoryId = multipleConnectionsResultNew.postHistoryId;
                int newLocalId = multipleConnectionsResultNew.localId;

                int newPostBlockTypeId = multipleConnectionsResultNew.postBlockTypeId;
                int newPossiblePredecessorsCount = multipleConnectionsResultNew.possiblePredecessorsCount;
                int newPossibleSuccessorsCount = multipleConnectionsResultNew.possibleSuccessorsCount;
                String newPossiblePredecessorLocalIds = multipleConnectionsResultNew.possiblePredecessorLocalIds;
                String newPossibleSuccessorLocalIds = multipleConnectionsResultNew.possibleSuccessorLocalIds;

                for (MultipleConnectionsResultOld multipleConnectionsResultOld : oldResults) {
                    int oldPostId = multipleConnectionsResultOld.postId;
                    int oldPostHistoryId = multipleConnectionsResultOld.postHistoryId;
                    int oldLocalId = multipleConnectionsResultOld.localId;

                    int oldPostBlockTypeId = multipleConnectionsResultOld.postBlockTypeId;
                    int oldNumberOfPossibleSuccessorsOrPredecessors = multipleConnectionsResultOld.numberOfPossibleSuccessorsOrPredecessors;
                    String oldPossiblePredOrSuccLocalIds = multipleConnectionsResultOld.possiblePredOrSuccLocalIds;

                    if (newPostId == oldPostId
                            && newPostHistoryId == oldPostHistoryId
                            && newLocalId == oldLocalId) {

                        assertEquals(newPostBlockTypeId, oldPostBlockTypeId);

                        if (oldPossiblePredOrSuccLocalIds.equals(newPossiblePredecessorLocalIds)) {
                            assertEquals(oldNumberOfPossibleSuccessorsOrPredecessors, newPossiblePredecessorsCount);
                        } else if (oldPossiblePredOrSuccLocalIds.equals(newPossibleSuccessorLocalIds)) {
                            assertEquals(oldNumberOfPossibleSuccessorsOrPredecessors, newPossibleSuccessorsCount);

                        } else {
                            logger.warning("Entry (" + newPostId + "," + newPostHistoryId + "," + newLocalId
                                    + ") in new file differs from old file with multiple possible connections.");
                        }

                        break;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MultipleConnectionsResultOld {
        int postId;
        int postHistoryId;
        int localId;
        int postBlockTypeId;
        String possiblePredOrSuccLocalIds;
        int numberOfPossibleSuccessorsOrPredecessors;

        MultipleConnectionsResultOld(int postId, int postHistoryId, int localId, int postBlockTypeId,
                                     String possiblePredOrSuccLocalIds,
                                     int numberOfPossibleSuccessorsOrPredecessors) {
            this.postId = postId;
            this.postHistoryId = postHistoryId;
            this.localId = localId;
            this.postBlockTypeId = postBlockTypeId;
            this.possiblePredOrSuccLocalIds = possiblePredOrSuccLocalIds;
            this.numberOfPossibleSuccessorsOrPredecessors = numberOfPossibleSuccessorsOrPredecessors;
        }
    }

    private class MultipleConnectionsResultNew {
        int postId;
        int postHistoryId;
        int localId;
        int postBlockTypeId;
        int possiblePredecessorsCount;
        int possibleSuccessorsCount;
        String possiblePredecessorLocalIds;
        String possibleSuccessorLocalIds;

        MultipleConnectionsResultNew(int postId, int postHistoryId, int localId, int postBlockTypeId,
                                     int possiblePredecessorsCount, int possibleSuccessorsCount,
                                     String possiblePredecessorLocalIds, String possibleSuccessorLocalIds) {
            this.postId = postId;
            this.postHistoryId = postHistoryId;
            this.localId = localId;
            this.postBlockTypeId = postBlockTypeId;
            this.possiblePredecessorsCount = possiblePredecessorsCount;
            this.possibleSuccessorsCount = possibleSuccessorsCount;
            this.possiblePredecessorLocalIds = possiblePredecessorLocalIds;
            this.possibleSuccessorLocalIds = possibleSuccessorLocalIds;
        }
    }


    @Test
    void checkSamples() {
        List<String> sampleUniqueSuffixes = new ArrayList<>();
        sampleUniqueSuffixes.add("17-06_sample_100_1");
        sampleUniqueSuffixes.add("17-06_sample_100_1+");
        sampleUniqueSuffixes.add("17-06_sample_100_2");
        sampleUniqueSuffixes.add("17-06_sample_100_2+");
        sampleUniqueSuffixes.add("Java_17-06_sample_100_1");
        sampleUniqueSuffixes.add("Java_17-06_sample_100_2");
        sampleUniqueSuffixes.add("17_06_sample_unclearMatching");
        // sampleUniqueSuffixes.add("17-06_sample_100_multiple_possible_links");

        for (String pathSuffix : sampleUniqueSuffixes) {

            MetricComparisonManager manager = MetricComparisonManager.create(
                    "TestManager",
                    Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_" + pathSuffix, "PostId_VersionCount_SO_" + pathSuffix + ".csv"),
                    Paths.get("testdata", "Samples_100","PostId_VersionCount_SO_" + pathSuffix, "files"),
                    Paths.get("testdata", "Samples_100", "PostId_VersionCount_SO_" + pathSuffix, "completed"),
                    false
            );

            List<PostGroundTruth> postsGroundTruths = new ArrayList<>(manager.getPostGroundTruth().values());
            List<PostVersionList> postVersionLists = new ArrayList<>(manager.getPostVersionLists().values());


            // check whether postIds correspond
            assertEquals(postsGroundTruths.size(), postVersionLists.size());

            for (PostVersionList postVersionList : postVersionLists) {
                int postId = postVersionList.getFirst().getPostId();
                boolean postIsInGT = false;
                for (PostGroundTruth postsGroundTruth : postsGroundTruths) {
                    int postIdGT = postsGroundTruth.getFirst().getPostId();
                    if (postId == postIdGT) {
                        postIsInGT = true;
                        break;
                    }
                }
                assertTrue(postIsInGT);
            }

            // check whether blocks correspond in type and local id
            for (PostVersionList postVersionList : postVersionLists) {
                int postId = postVersionList.getFirst().getPostId();

                for (PostGroundTruth postsGroundTruth : postsGroundTruths) {
                    int postIdGT = postsGroundTruth.getFirst().getPostId();
                    if (postId == postIdGT) {

                        List<PostBlockConnection> postBlockConnectionSet_postVersionList = new LinkedList<>(postVersionList.getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters()));
                        List<PostBlockConnection> postBlockConnectionSet_postGroundTruth = new LinkedList<>(postsGroundTruth.getConnections(PostBlockVersion.getAllPostBlockTypeIdFilters()));

                        for(int i=0; i<postBlockConnectionSet_postVersionList.size(); i++){
                            assertEquals(
                                    postBlockConnectionSet_postVersionList.get(i).getRight().getLocalId(),
                                    postBlockConnectionSet_postGroundTruth.get(i).getRight().getLocalId());

                            assertEquals(
                                    postBlockConnectionSet_postVersionList.get(i).getRight().getPostBlockTypeId(),
                                    postBlockConnectionSet_postGroundTruth.get(i).getRight().getPostBlockTypeId());
                        }

                        break;
                    }
                }
            }
        }

    }
}
