package de.unitrier.st.soposthistory.tests;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.*;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BlockLifeSpanAndGroundTruthTest {
    private static Path pathToPostIdList = Paths.get("testdata", "postIds.csv");
    private static Path pathToPostHistory = Paths.get("testdata");
    private static Path pathToGroundTruth = Paths.get("testdata", "gt");
    private static Path outputDir = Paths.get("testdata", "metrics comparison");

    @Test
    void testReadFromDirectory() {
        List<PostGroundTruth> postGroundTruthList = PostGroundTruth.readFromDirectory(pathToGroundTruth);
        try {
            assertEquals(Files.list(pathToGroundTruth).filter(
                    file -> PostGroundTruth.fileNamePattern.matcher(file.toFile().getName()).matches())
                            .count(),
                    postGroundTruthList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testPostBlockLifeSpanVersionsEqual(){
        // compare two PostBlockLifeSpanVersions
        PostBlockLifeSpanVersion original = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostId = new PostBlockLifeSpanVersion(4712, 42, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostHistoryId = new PostBlockLifeSpanVersion(4711, 43, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostBlockTypeId = new PostBlockLifeSpanVersion(4711, 42, 2, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentLocalId = new PostBlockLifeSpanVersion(4711, 42, 1, 1, 0, 0, "");
        PostBlockLifeSpanVersion differentPredId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 1, 0,"");
        PostBlockLifeSpanVersion differentSuccId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 1, "");

        assertTrue(original.equals(original));
        assertFalse(original.equals(differentPostId));
        assertFalse(original.equals(differentPostHistoryId));
        assertFalse(original.equals(differentPostBlockTypeId));
        assertFalse(original.equals(differentLocalId));
        assertTrue(original.equals(differentPredId));
        assertTrue(original.equals(differentSuccId));
    }

    @Test
    void testPostBlockLifeSpanExtraction() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        List<PostBlockLifeSpan> lifeSpans = a_22037280.getPostBlockLifeSpans();
        List<PostBlockLifeSpan> lifeSpansGT = a_22037280_gt.getPostBlockLifeSpans();
        assertEquals(lifeSpans.size(), lifeSpansGT.size());
        assertEquals(5, lifeSpans.size());
        for (int i=0; i<lifeSpans.size(); i++) {
            assertTrue(lifeSpans.get(i).equals(lifeSpansGT.get(i)));
        }
    }

    @Test
    void testPostBlockLifeSpanExtractionFilter() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        // text
        List<PostBlockLifeSpan> textBlockLifeSpans = a_22037280.getPostBlockLifeSpans(
                TextBlockVersion.getPostBlockTypeIdFilter()
        );
        List<PostBlockLifeSpan> textLifeSpansGT = a_22037280_gt.getPostBlockLifeSpans(
                TextBlockVersion.getPostBlockTypeIdFilter()
        );
        assertEquals(textBlockLifeSpans.size(), textLifeSpansGT.size());
        assertEquals(3, textBlockLifeSpans.size());
        for (int i=0; i<textBlockLifeSpans.size(); i++) {
            assertTrue(textBlockLifeSpans.get(i).equals(textLifeSpansGT.get(i)));
        }

        // code
        List<PostBlockLifeSpan> codeBlockLifeSpans = a_22037280.getPostBlockLifeSpans(
                CodeBlockVersion.getPostBlockTypeIdFilter()
        );
        List<PostBlockLifeSpan> codeLifeSpansGT = a_22037280_gt.getPostBlockLifeSpans(
                CodeBlockVersion.getPostBlockTypeIdFilter()
        );
        assertEquals(codeBlockLifeSpans.size(), codeLifeSpansGT.size());
        assertEquals(2, codeBlockLifeSpans.size());
        for (int i=0; i<codeBlockLifeSpans.size(); i++) {
            assertTrue(codeBlockLifeSpans.get(i).equals(codeLifeSpansGT.get(i)));
        }
    }

    @Test
    void testPostBlockConnectionExtraction() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        List<PostBlockLifeSpan> lifeSpans = a_22037280.getPostBlockLifeSpans();
        List<PostBlockLifeSpan> lifeSpansGT = a_22037280_gt.getPostBlockLifeSpans();

        Set<PostBlockConnection> connections = PostBlockLifeSpan.toPostBlockConnections(lifeSpans);
        Set<PostBlockConnection> connectionsGT = PostBlockLifeSpan.toPostBlockConnections(lifeSpansGT);

        assertEquals(connections.size(), connectionsGT.size());
        assertTrue(PostBlockConnection.equals(connections, connectionsGT));

        assertTrue(PostBlockConnection.equals(
                PostBlockConnection.intersection(connections, connectionsGT),
                connections)
        );

        assertTrue(PostBlockConnection.equals(
                PostBlockConnection.union(connections, connectionsGT),
                connections)
        );

        assertEquals(0, PostBlockConnection.difference(connections, connectionsGT).size());
    }

    @Test
    void testPostBlockPossibleConnectionsComparison() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        assertEquals(78, a_22037280.getPossibleConnections());
        assertEquals(a_22037280.getPossibleConnections(), a_22037280_gt.getPossibleConnections());
    }

    @Test
    void testPostBlockConnectionEquals() {
        PostBlockLifeSpanVersion v1_1 = new PostBlockLifeSpanVersion(1, 1, 1, 1);
        PostBlockLifeSpanVersion v1_2 = new PostBlockLifeSpanVersion(1, 1, 1, 1);
        PostBlockLifeSpanVersion v2 = new PostBlockLifeSpanVersion(1, 2, 1, 1);
        PostBlockLifeSpanVersion v3 = new PostBlockLifeSpanVersion(1, 3, 1, 1);
        PostBlockLifeSpanVersion v4 = new PostBlockLifeSpanVersion(1, 4, 1, 1);

        // test equality of PostBlockLifeSpanVersions
        assertEquals(v1_1.getPostId(), v1_2.getPostId());
        assertEquals(v1_1.getPostHistoryId(), v1_2.getPostHistoryId());
        assertEquals(v1_1.getPostBlockTypeId(), v1_2.getPostBlockTypeId());
        assertEquals(v1_1.getLocalId(), v1_2.getLocalId());

        // test equality of PostBlockConnections
        PostBlockConnection connection1 = new PostBlockConnection(v1_1, v2);
        PostBlockConnection connection2 = new PostBlockConnection(v1_2, v2);
        assertTrue(connection1.equals(connection2));

        // test equaliy of a set of PostBlockConnections
        PostBlockConnection connection3 = new PostBlockConnection(v1_2, v2);
        PostBlockConnection connection4 = new PostBlockConnection(v3, v4);
        assertTrue(PostBlockConnection.equals(
                Sets.newHashSet(connection1, connection2, connection3, connection4),
                Sets.newHashSet(connection1, connection2, connection3, connection4))
        );
    }

    @Test
    void testGetConnections(){
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        List<Integer> postVersionListPostHistoryIds = a_22037280.getPostHistoryIds();
        List<Integer> groundTruthPostHistoryIds = a_22037280_gt.getPostHistoryIds();

        assertEquals(postVersionListPostHistoryIds, groundTruthPostHistoryIds);

        for (Integer postHistoryId : groundTruthPostHistoryIds) {
            Set<PostBlockConnection> postBlockConnections = a_22037280.getPostVersion(postHistoryId).getConnections();
            Set<PostBlockConnection> postBlockConnectionsGT = a_22037280_gt.getConnections(postHistoryId);

            assertTrue(PostBlockConnection.equals(postBlockConnections, postBlockConnectionsGT));
        }
    }

    @Test
    void testProcessVersionHistoryWithIntermediateResetting() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2, false);

        testPostBlockVersionHistoryReset(a_22037280);
        a_22037280.processVersionHistory();
        testPostBlockVersionHistoryProcessed(a_22037280);
        a_22037280.resetPostBlockVersionHistory();
        testPostBlockVersionHistoryReset(a_22037280);
        a_22037280.processVersionHistory();
        testPostBlockVersionHistoryProcessed(a_22037280);
    }

    private void testPostBlockVersionHistoryReset(PostVersionList postVersionList) {
        for (PostVersion currentPostVersion : postVersionList) {
            for (PostBlockVersion currentPostBlockVersion : currentPostVersion.getPostBlocks()) {
                assertNull(currentPostBlockVersion.getRootPostBlockId());
                assertNull(currentPostBlockVersion.getPredPostBlockId());
                assertNull(currentPostBlockVersion.getPredEqual());
                assertNull(currentPostBlockVersion.getPredSimilarity());
                assertEquals(0, currentPostBlockVersion.getPredCount());
                assertEquals(0, currentPostBlockVersion.getSuccCount());
                assertNull(currentPostBlockVersion.getPred());
                assertNull(currentPostBlockVersion.getSucc());
                assertNull(currentPostBlockVersion.getRootPostBlock());
                assertNull(currentPostBlockVersion.getPredDiff());
                assertTrue(currentPostBlockVersion.isAvailable());
                assertEquals(0, currentPostBlockVersion.getMatchingPredecessors().size());
                assertEquals(0, currentPostBlockVersion.getPredecessorSimilarities().size());
                assertEquals(-1.0, currentPostBlockVersion.getMaxSimilarity());
                assertEquals(-1.0, currentPostBlockVersion.getSimilarityThreshold());
                assertFalse(currentPostBlockVersion.isLifeSpanExtracted());
            }
        }
    }

    private void testPostBlockVersionHistoryProcessed(PostVersionList postVersionList) {
        for (PostVersion currentPostVersion : postVersionList) {
            for (PostBlockVersion currentPostBlockVersion : currentPostVersion.getPostBlocks()) {
                assertNotNull(currentPostBlockVersion.getRootPostBlockId());
                assertNotNull(currentPostBlockVersion.getRootPostBlock());
            }
        }
    }

    @Test
    void testMetricComparisonManager() {
        MetricComparisonManager manager = MetricComparisonManager.create(
                "TestManager", pathToPostIdList, pathToPostHistory, pathToGroundTruth, false
        );

        assertEquals(manager.getPostVersionLists().size(), manager.getPostGroundTruth().size());
        assertThat(manager.getPostVersionLists().keySet(), is(manager.getPostGroundTruth().keySet()));

        manager.addSimilarityMetric(
                "fourGramOverlap",
                de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap
        );
        manager.addSimilarityThreshold(0.6);

        manager.compareMetrics();

        List<Integer> postHistoryIds_3758880 = manager.getPostGroundTruth().get(3758880).getPostHistoryIds();
        MetricComparison comparison_a_3758880 = manager.getMetricComparison(3758880, "fourGramOverlap", 0.6);


        /* compare a 3758880 */
        // first version has never predecessors
        int version_1_id = postHistoryIds_3758880.get(0);
        assertEquals(new Integer(0), comparison_a_3758880.getTruePositivesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getTrueNegativesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesText().get(version_1_id));

        assertEquals(new Integer(0), comparison_a_3758880.getTruePositivesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getTrueNegativesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesCode().get(version_1_id));


        // second version
        int version_id = postHistoryIds_3758880.get(1);
        assertEquals(new Integer(1), comparison_a_3758880.getTruePositivesText().get(version_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesText().get(version_id));
        assertEquals(new Integer(5), comparison_a_3758880.getTrueNegativesText().get(version_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesText().get(version_id));

        assertEquals(new Integer(2), comparison_a_3758880.getTruePositivesCode().get(version_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesCode().get(version_id));
        assertEquals(new Integer(4), comparison_a_3758880.getTrueNegativesCode().get(version_id));
        assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesCode().get(version_id));

        // version 3 to 10 only for text blocks (they don't differ)
        for(int i=2; i<10; i++){
            int version_2_to_11_id_text = postHistoryIds_3758880.get(i);
            assertEquals(new Integer(2), comparison_a_3758880.getTruePositivesText().get(version_2_to_11_id_text));
            assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesText().get(version_2_to_11_id_text));
            assertEquals(new Integer(2), comparison_a_3758880.getTrueNegativesText().get(version_2_to_11_id_text));
            assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesText().get(version_2_to_11_id_text));
        }

        int version_11_id_text = postHistoryIds_3758880.get(10);
        assertEquals(new Integer(2), comparison_a_3758880.getTruePositivesText().get(version_11_id_text));
        assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesText().get(version_11_id_text));
        assertEquals(new Integer(4), comparison_a_3758880.getTrueNegativesText().get(version_11_id_text));
        assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesText().get(version_11_id_text));

        // version 3 and 6 for code
        List<Integer> versionsOfType1 = Arrays.asList(2,5);
        for (Integer version : versionsOfType1) {
            int version_3_or_6_id = postHistoryIds_3758880.get(version);
            assertEquals(new Integer(1), comparison_a_3758880.getTruePositivesCode().get(version_3_or_6_id));
            assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesCode().get(version_3_or_6_id));
            assertEquals(new Integer(2), comparison_a_3758880.getTrueNegativesCode().get(version_3_or_6_id));
            assertEquals(new Integer(1), comparison_a_3758880.getFalseNegativesCode().get(version_3_or_6_id));
        }

        // version 4,5,7,8,9,10,11 for code
        List<Integer> versionsOfType2 = Arrays.asList(3,4,6,7,8,9,10);
        for (Integer version : versionsOfType2) {
            int version_i_id = postHistoryIds_3758880.get(version);
            assertEquals(new Integer(2), comparison_a_3758880.getTruePositivesCode().get(version_i_id));
            assertEquals(new Integer(0), comparison_a_3758880.getFalsePositivesCode().get(version_i_id));
            assertEquals(new Integer(2), comparison_a_3758880.getTrueNegativesCode().get(version_i_id));
            assertEquals(new Integer(0), comparison_a_3758880.getFalseNegativesCode().get(version_i_id));
        }



        /* compare a 22037280 */
        List<Integer> postHistoryIds_22037280 = manager.getPostGroundTruth().get(22037280).getPostHistoryIds();
        MetricComparison comparison_a_22037280 = manager.getMetricComparison(22037280, "fourGramOverlap", 0.6);

        version_1_id = postHistoryIds_22037280.get(0);

        assertEquals(new Integer(0), comparison_a_22037280.getTruePositivesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getFalsePositivesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getTrueNegativesText().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getFalseNegativesText().get(version_1_id));

        assertEquals(new Integer(0), comparison_a_22037280.getTruePositivesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getFalsePositivesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getTrueNegativesCode().get(version_1_id));
        assertEquals(new Integer(0), comparison_a_22037280.getFalseNegativesCode().get(version_1_id));

        for(int i=1; i<postHistoryIds_22037280.size(); i++) {
            version_id = postHistoryIds_22037280.get(i);

            assertEquals(new Integer(3), comparison_a_22037280.getTruePositivesText().get(version_id));
            assertEquals(new Integer(0), comparison_a_22037280.getFalsePositivesText().get(version_id));
            assertEquals(new Integer(6), comparison_a_22037280.getTrueNegativesText().get(version_id));
            assertEquals(new Integer(0), comparison_a_22037280.getFalseNegativesText().get(version_id));

            assertEquals(new Integer(2), comparison_a_22037280.getTruePositivesCode().get(version_id));
            assertEquals(new Integer(0), comparison_a_22037280.getFalsePositivesCode().get(version_id));
            assertEquals(new Integer(2), comparison_a_22037280.getTrueNegativesCode().get(version_id));
            assertEquals(new Integer(0), comparison_a_22037280.getFalseNegativesCode().get(version_id));
        }

        manager.writeToCSV(outputDir);
    }


    @Test
    void testCompareMetricComparisonManagerWithComparisonFromOldProject() {
        MetricComparisonManager manager = MetricComparisonManager.create(
                "TestManager", pathToPostIdList, pathToPostHistory, pathToGroundTruth, true
        );

        List<Integer> postHistoryIds_3758880 = manager.getPostVersionLists().get(3758880).getPostHistoryIds();
        List<Integer> postHistoryIds_22037280 = manager.getPostVersionLists().get(22037280).getPostHistoryIds();

        manager.compareMetrics();
        manager.writeToCSV(Paths.get("testdata","metrics comparison"));

        CSVParser csvParser = null;

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
                }catch (NumberFormatException e){}

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

    @Test
    void testGetPossibleConnections() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        assertEquals(7, a_22037280.size());

        assertEquals(a_22037280.getPossibleConnections(), a_22037280_gt.getPossibleConnections());

        for (PostVersion postVersion : a_22037280) {
            assertEquals(3, postVersion.getTextBlocks().size());
            assertEquals(2, postVersion.getCodeBlocks().size());
        }

        Set<Integer> set = new HashSet<>();
        assertEquals(0, a_22037280.getPossibleConnections(set));

        int possibleTextConnections = a_22037280.getPossibleConnections(TextBlockVersion.getPostBlockTypeIdFilter());
        assertEquals(6 * 9, possibleTextConnections); // 6 versions with each 9=3*3 possible text connections

        int possibleCodeConnections = a_22037280.getPossibleConnections(CodeBlockVersion.getPostBlockTypeIdFilter());
        assertEquals(6 * 4, possibleCodeConnections); // 6 versions with each 4=2*2 possible code connections

        int possibleConnections = a_22037280.getPossibleConnections(PostBlockVersion.getAllPostBlockTypeIdFilters());
        assertEquals(6 * 4 + 6 * 9, possibleConnections); // 6 versions with each 4=2*2 and 9=3*3 possible connections

        // compare results of getPossibleConnections() for PostVersion and PostVersionList
        possibleConnections = 0;
        for (PostVersion current : a_22037280) {
            possibleConnections += current.getPossibleConnections();
        }
        assertEquals(a_22037280.getPossibleConnections(), possibleConnections);

        // check if post version pred and succ assignments are also set in case post history has not been processed yet
        possibleConnections = 0;
        for (PostVersion current : a_22037280) {
            possibleConnections += current.getPossibleConnections();
        }
        assertEquals(a_22037280.getPossibleConnections(), possibleConnections);
    }

//    @Test
//    void testNumberOfPredecessorsOfOnePost() {
//        int postId = 3758880;
//        PostVersionList a_3758880 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2, false);
//        PostGroundTruth a_3758880_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);
//
//        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(
//                PostVersionList.PostBlockTypeFilter.TEXT,
//                Config.DEFAULT.withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice));
//
//        List<TextBlockVersion> textBlocks = postVersionsListManagement.getPostVersionListWithID(postId).get(postVersionsListManagement.getPostVersionListWithID(postId).size() - 1).getTextBlocks();
//        assertEquals(new Integer(1), textBlocks.get(0).getPred().getLocalId());
//        assertEquals(new Integer(1), textBlocks.get(0).getLocalId());
//
//        assertEquals(new Integer(3), textBlocks.get(1).getPred().getLocalId());
//        assertEquals(new Integer(3), textBlocks.get(1).getLocalId());
//
//        assertEquals(null, textBlocks.get(2).getPred());
//        assertEquals(new Integer(5), textBlocks.get(2).getLocalId());
//    }
    @Test
    void testNumberOfPredecessorsOfOnePost() {
        // this checks whether a block can be predecessor of more than one block by choosing a very low threshold.

        int postId = 3758880;
        PostVersionList a_3758880 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2, false);

        a_3758880.processVersionHistory(
                Config.DEFAULT
                        .withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice)
                        .withCodeSimilarityThreshold(0.01),
                TextBlockVersion.getPostBlockTypeIdFilter());

        List<TextBlockVersion> textBlocks = a_3758880.getLast().getTextBlocks();
        assertEquals(new Integer(1), textBlocks.get(0).getPred().getLocalId());
        assertEquals(new Integer(1), textBlocks.get(0).getLocalId());

        assertEquals(new Integer(3), textBlocks.get(1).getPred().getLocalId());
        assertEquals(new Integer(3), textBlocks.get(1).getLocalId());

        assertEquals(null, textBlocks.get(2).getPred());
        assertEquals(new Integer(5), textBlocks.get(2).getLocalId());
    }

//    @Test
//    void testGroundTruthExtractionOfCSV(){
//
//        // testing text
//        assertNull(groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(4711));    // post with id 4711 is not listed in ground truth
//        assertEquals(2, groundTruthExtractionOfCSVs.getGroundTruthText().size());
//
//        ConnectionsOfAllVersions connectionsOfAllVersions_text = groundTruthExtractionOfCSVs.getGroundTruthText().getFirst();
//        assertEquals(connectionsOfAllVersions_text, groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(3758880));
//        assertEquals(10, connectionsOfAllVersions_text.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions
//
//        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getLeftLocalId());
//        assertEquals(new Integer(1), connectionsOfAllVersions_text.get(0).get(0).getRightLocalId());
//        assertEquals(1, connectionsOfAllVersions_text.get(0).get(0).getPostBlockTypeId());
//        assertNull(connectionsOfAllVersions_text.get(0).get(1).getLeftLocalId());
//        assertEquals(new Integer(3),connectionsOfAllVersions_text.get(0).get(1).getRightLocalId());
//
//        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getLeftLocalId());
//        assertEquals(new Integer(3), connectionsOfAllVersions_text.get(7).get(1).getRightLocalId());
//
//
//        // testing code
//        ConnectionsOfAllVersions connectionsOfAllVersions_code = groundTruthExtractionOfCSVs.getGroundTruthCode().getFirst();
//        assertEquals(10, connectionsOfAllVersions_code.size());       // post version list has 11 versions and therefore 10 comparisons for adjacent versions
//
//        assertEquals(new Integer(6), connectionsOfAllVersions_code.get(0).get(1).getLeftLocalId()); // compares always from right to left
//        assertEquals(new Integer(4), connectionsOfAllVersions_code.get(0).get(1).getRightLocalId());
//
//        assertEquals(2, connectionsOfAllVersions_code.get(2).get(1).getPostBlockTypeId());
//        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getLeftLocalId());
//        assertEquals(new Integer(4),connectionsOfAllVersions_code.get(2).get(1).getRightLocalId());
//    }
//
//    @Test
//    void testNumberOfPredecessorsComputedMetric() {
//
//        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
//
//        for (PostVersionList postVersionList : postVersionsListManagement.postVersionLists) {
//            postVersionList.processVersionHistory(Config.DEFAULT.withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice));
//
//            for (PostVersion postVersion : postVersionList) {
//                List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();
//
//                for (int i = 0; i < postBlocks.size(); i++) {
//                    if (postBlocks.get(i).getPred() == null)
//                        continue;
//
//                    for (int j = i + 1; j < postBlocks.size(); j++) {
//                        if (postBlocks.get(j).getPred() == null || postBlocks.get(i) instanceof TextBlockVersion != postBlocks.get(j) instanceof TextBlockVersion)
//                            continue;
//
//                        assertNotEquals(postBlocks.get(i).getPred().getLocalId(), postBlocks.get(j).getPred().getLocalId());
//                    }
//                }
//
//            }
//        }
//    }
//
//    @Test
//    void testNumberOfPredecessorsGroundTruth() {
//
//        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(Paths.get("testdata","Samples_test",  "representative CSVs").toString());
//
//        for (ConnectionsOfAllVersions connectionsOfAllVersions : groundTruthExtractionOfCSVs.getPostGroundTruth()) {
//            for (ConnectionsOfTwoVersions connectionsOfTwoVersions : connectionsOfAllVersions) {
//
//                for (int i = 0; i < connectionsOfTwoVersions.size(); i++) {
//                    if (connectionsOfTwoVersions.get(i).getLeftLocalId() == null)
//                        continue;
//
//                    for (int j = i + 1; j < connectionsOfTwoVersions.size(); j++) {
//                        if (connectionsOfTwoVersions.get(j).getLeftLocalId() == null ||
//                                connectionsOfTwoVersions.get(i).getPostBlockTypeId() != connectionsOfTwoVersions.get(j).getPostBlockTypeId())
//                            continue;
//
//                        assertNotEquals(connectionsOfTwoVersions.get(i).getLeftLocalId(), connectionsOfTwoVersions.get(j).getLeftLocalId());
//                    }
//                }
//
//            }
//        }
//    }
//
//    @Test
//    void checkWhetherPostVersionListConnectionsWillBeResetRight() {
//        int postId = 3758880;
//        //TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTokenDiceVariant;
//
//        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
//        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);
//
//
//        // This sets predecessors
//        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
//
//        // This resets the predecessors again
//        postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
//        connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
//        for (ConnectionsOfTwoVersions connections : connectionsOfAllVersionsComputedMetric_text) {
//            for (ConnectedBlocks connection : connections) {
//                assertNull(connection.getLeftLocalId());
//                assertNotNull(connection.getRightLocalId());
//            }
//        }
//    }
}
