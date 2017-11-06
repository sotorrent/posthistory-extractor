package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.gt.MetricComparison;
import de.unitrier.st.soposthistory.gt.MetricComparisonManager;
import de.unitrier.st.soposthistory.gt.Statistics;
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
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class DisabledTests {
    private static Path pathToOldMetricComparisons = Paths.get(
            "testdata", "metrics_comparison", "results_metric_comparison_old.csv"
    );

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

                    assert postHistoryIds != null;
                    for (Integer tmpPostHistoryId : postHistoryIds) {
                        assertNull(tmpMetricComparison.getTruePositivesText().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getFalsePositivesText().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getTrueNegativesText().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getFalseNegativesText().get(tmpPostHistoryId));

                        assertNull(tmpMetricComparison.getTruePositivesCode().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getFalsePositivesCode().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getTrueNegativesCode().get(tmpPostHistoryId));
                        assertNull(tmpMetricComparison.getFalseNegativesCode().get(tmpPostHistoryId));
                    }
                } else {
                    // TODO: Check true negatives for text and code
                    assertEquals(tmpMetricComparison.getTruePositivesText().get(postHistoryId), truePositivesText);
                    assertEquals(tmpMetricComparison.getFalsePositivesText().get(postHistoryId), falsePositivesText);
                    assertEquals(tmpMetricComparison.getFalseNegativesText().get(postHistoryId), falseNegativesText);

                    assertEquals(tmpMetricComparison.getTruePositivesCode().get(postHistoryId), truePositivesCode);
                    assertEquals(tmpMetricComparison.getFalsePositivesCode().get(postHistoryId), falsePositivesCode);
                    assertEquals(tmpMetricComparison.getFalseNegativesCode().get(postHistoryId), falseNegativesCode);
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
                            System.out.println(oldPostId + ", " + oldPostHistoryId + ", " + oldLocalId + ", " + oldPossiblePredOrSuccLocalIds);
                            System.out.println(newPostId + ", " + newPostHistoryId + ", " + newLocalId + ", " + newPossiblePredecessorLocalIds + ", " + newPossibleSuccessorLocalIds);
                            System.out.println();
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
}
