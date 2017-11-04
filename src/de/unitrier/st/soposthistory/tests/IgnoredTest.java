package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.gt.MetricComparison;
import de.unitrier.st.soposthistory.gt.MetricComparisonManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Ignore
class IgnoredTest extends BlockLifeSpanAndGroundTruthTest{

    @Test
    void testCompareMetricComparisonManagerWithComparisonFromOldProject() {
        MetricComparisonManager manager = MetricComparisonManager.create(
                "TestManager", pathToPostIdList, pathToPostHistory, pathToGroundTruth, true
        );

        List<Integer> postHistoryIds_3758880 = manager.getPostVersionLists().get(3758880).getPostHistoryIds();
        List<Integer> postHistoryIds_22037280 = manager.getPostVersionLists().get(22037280).getPostHistoryIds();

        manager.compareMetrics();
        manager.writeToCSV(Paths.get("testdata","metrics comparison"));

        CSVParser csvParser;

        try {
            csvParser = CSVParser.parse(
                    Paths.get("testdata", "metrics comparison", "resultsMetricComparisonOldProject.csv").toFile(),
                    StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT.withHeader("sample", "metric", "threshold", "postid", "posthistoryid", "runtimetext", "#truepositivestext", "#truenegativestext",
                            "#falsepositivestext", "#falsenegativestext", "#runtimecode", "#truepositivescode", "#truenegativescode",
                            "#falsepositivescode", "#falsenegativescode").withDelimiter(';')
                            .withQuote('"')
                            .withQuoteMode(QuoteMode.MINIMAL)
                            .withEscape('\\')
                            .withFirstRecordAsHeader()
            );

            csvParser.getHeaderMap();
            List<CSVRecord> records = csvParser.getRecords();
            for(CSVRecord record : records){
                String metric = record.get("metric");

                Double threshold = Double.valueOf(record.get("threshold"));
                if((int)(threshold*100) % 10 != 0) { // Comparison manager computes only equal thresholds so unequal thresholds will be skipped
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

                try{
                    postHistoryId = Integer.valueOf(record.get("posthistoryid"));

                    truePositivesText = Integer.valueOf(record.get("#truepositivestext"));
                    trueNegativesText = Integer.valueOf(record.get("#truenegativestext"));
                    falsePositivesText = Integer.valueOf(record.get("#falsepositivestext"));
                    falseNegativesText = Integer.valueOf(record.get("#falsenegativestext"));

                    truePositivesCode = Integer.valueOf(record.get("#truepositivescode"));
                    trueNegativesCode = Integer.valueOf(record.get("#truenegativescode"));
                    falsePositivesCode = Integer.valueOf(record.get("#falsepositivescode"));
                    falseNegativesCode = Integer.valueOf(record.get("#falsenegativescode"));
                }catch (NumberFormatException ignored){}

                MetricComparison tmpMetricComparison = manager.getMetricComparison(postId, metric, threshold);


                if(postHistoryId == null) {
                    List<Integer> postHistoryIds = null;
                    if(postId == 3758880) {
                        postHistoryIds = postHistoryIds_3758880;
                    }else if(postId == 22037280){
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

}
