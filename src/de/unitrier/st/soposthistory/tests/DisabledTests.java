package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.gt.MetricComparison;
import de.unitrier.st.soposthistory.gt.MetricComparisonManager;
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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
class DisabledTests {
    private static Path pathToOldMetricComparisons = Paths.get(
            "testdata", "metrics_comparison", "results_metric_comparison_old.csv"
    );

    private static Path rootPathToLargeSamples = Paths.get("testdata","samples_10000");
    private static List<Path> pathsToLargeSamplesFiles = new LinkedList<>();

    static {
        for(int i=1; i<=10; i++) {
            pathsToLargeSamplesFiles.add(Paths.get(rootPathToLargeSamples.toString(), "PostId_VersionCount_SO_17-06_sample_10000_" + i, "files"));
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
                // TODO: Lorik: What do you mean by "equal thresholds"
                // comparison manager computes only equal thresholds so unequal thresholds will be skipped
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
    void testLargeSamplesParsable() throws IOException {
        for (Path currentPath : pathsToLargeSamplesFiles) {
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
        CSVParser csvParserOld, csvParserNew;
        try {
            // parse old records
            csvParserOld = CSVParser.parse(
                    Paths.get(rootPathToLargeSamples.toString(),"possible_multiple_connections_old.csv").toFile(),
                    StandardCharsets.UTF_8,
                    MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
            );
            csvParserOld.getHeaderMap();
            List<CSVRecord> oldRecords = csvParserOld.getRecords();

            // parse new records
            csvParserNew = CSVParser.parse(
                    Paths.get(rootPathToLargeSamples.toString(),"possible_multiple_connections.csv").toFile(),
                    StandardCharsets.UTF_8,
                    MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
            );
            csvParserNew.getHeaderMap();
            List<CSVRecord> newRecords = csvParserNew.getRecords();

            for(int i=0; i<oldRecords.size(); i++) {
                CSVRecord recordOld = oldRecords.get(i);
                CSVRecord recordNew = newRecords.get(i);
                assertEquals(recordOld, recordNew);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
