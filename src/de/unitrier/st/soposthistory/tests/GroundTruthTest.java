package de.unitrier.st.soposthistory.tests;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpan;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpanVersion;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GroundTruthTest {
    private static Path pathToPostHistory = Paths.get("testdata", "gt_test", "files");
    private static Path pathToGroundTruth = Paths.get("testdata", "gt_test", "gt");

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
    void testPostBlockLifeSpanVersionsEqual() {
        // compare two PostBlockLifeSpanVersions
        PostBlockLifeSpanVersion original = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostId = new PostBlockLifeSpanVersion(4712, 42, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostHistoryId = new PostBlockLifeSpanVersion(4711, 43, 1, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentPostBlockTypeId = new PostBlockLifeSpanVersion(4711, 42, 2, 0, 0, 0, "");
        PostBlockLifeSpanVersion differentLocalId = new PostBlockLifeSpanVersion(4711, 42, 1, 1, 0, 0, "");
        PostBlockLifeSpanVersion differentPredId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 1, 0, "");
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
        for (int i = 0; i < lifeSpans.size(); i++) {
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
        for (int i = 0; i < textBlockLifeSpans.size(); i++) {
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
        for (int i = 0; i < codeBlockLifeSpans.size(); i++) {
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
    void testPostBlockPossibleComparisonsAnswer22037280() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        assertEquals(78, a_22037280.getPossibleComparisons());
        assertEquals(a_22037280.getPossibleComparisons(), a_22037280_gt.getPossibleComparisons());
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

        // test equality of a set of PostBlockConnections
        PostBlockConnection connection3 = new PostBlockConnection(v1_2, v2);
        PostBlockConnection connection4 = new PostBlockConnection(v3, v4);
        assertTrue(PostBlockConnection.equals(
                Sets.newHashSet(connection1, connection2, connection3, connection4),
                Sets.newHashSet(connection1, connection2, connection3, connection4))
        );

        // test PostBlockConnection.union
        assertTrue(PostBlockConnection.equals(
                PostBlockConnection.union(Sets.newHashSet(connection1, connection2),
                        Sets.newHashSet(connection3, connection4)),
                Sets.newHashSet(connection1, connection3, connection4))
        );
    }

    @Test
    void testGetConnections() {
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
        assertNull(a_22037280.get(1).getTextBlocks().get(0).getPred()); // predecessors of post blocks have not been set yet
        a_22037280.processVersionHistory();
        assertNotNull(a_22037280.get(1).getTextBlocks().get(0).getPred()); // predecessors of post blocks have been set
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
                assertEquals(-1.0, currentPostBlockVersion.getMaxBackupSimilarity());
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
    void testGetPossibleComparisons() {
        int postId = 22037280;
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToPostHistory, postId, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGroundTruth, postId);

        assertEquals(7, a_22037280.size());

        assertEquals(a_22037280.getPossibleComparisons(), a_22037280_gt.getPossibleComparisons());

        for (PostVersion postVersion : a_22037280) {
            assertEquals(3, postVersion.getTextBlocks().size());
            assertEquals(2, postVersion.getCodeBlocks().size());
        }

        Set<Integer> set = new HashSet<>();
        assertEquals(0, a_22037280.getPossibleComparisons(set));

        int possibleTextConnections = a_22037280.getPossibleComparisons(TextBlockVersion.getPostBlockTypeIdFilter());
        assertEquals(6 * 9, possibleTextConnections); // 6 versions with each 9=3*3 possible text comparisons

        int possibleCodeConnections = a_22037280.getPossibleComparisons(CodeBlockVersion.getPostBlockTypeIdFilter());
        assertEquals(6 * 4, possibleCodeConnections); // 6 versions with each 4=2*2 possible code comparisons

        int possibleComparisons = a_22037280.getPossibleComparisons(PostBlockVersion.getAllPostBlockTypeIdFilters());
        assertEquals(6 * 4 + 6 * 9, possibleComparisons); // 6 versions with each 4=2*2 and 9=3*3 possible comparisons

        // compare results of getPossibleComparisons() for PostVersion and PostVersionList
        possibleComparisons = 0;
        for (PostVersion current : a_22037280) {
            possibleComparisons += current.getPossibleComparisons();
        }
        assertEquals(a_22037280.getPossibleComparisons(), possibleComparisons);

        // check if post version pred and succ assignments are also set in case post history has not been processed yet
        possibleComparisons = 0;
        for (PostVersion current : a_22037280) {
            possibleComparisons += current.getPossibleComparisons();
        }
        assertEquals(a_22037280.getPossibleComparisons(), possibleComparisons);
    }

    @Test
    void testNumberOfPredecessorsOfOnePost() {
        // this checks whether a block can be predecessor of more than one block when choosing a very low threshold.

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


    @Test
    void testNumberOfPredecessorsComputedMetric() {
        // This test case uses very low thresholds to provoke multiple possible connection between post blocks.
        // Then it checks whether a block will be set more than once as a predecessor (which should not happen).

        List<PostVersionList> postVersionLists = PostVersionList.readFromDirectory(pathToPostHistory);

        for (PostVersionList postVersionList : postVersionLists) {
            postVersionList.processVersionHistory(
                    Config.DEFAULT
                            .withTextSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice)
                            .withCodeSimilarityMetric(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice)
                            .withTextSimilarityThreshold(0.01)
                            .withCodeSimilarityThreshold(0.01)
            );

            for (PostVersion postVersion : postVersionList) {
                List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();

                for (int i = 0; i < postBlocks.size(); i++) {
                    if (postBlocks.get(i).getPred() == null)
                        continue;

                    for (int j = i + 1; j < postBlocks.size(); j++) {
                        if (postBlocks.get(j).getPred() == null || postBlocks.get(i) instanceof TextBlockVersion != postBlocks.get(j) instanceof TextBlockVersion)
                            continue;

                        assertNotEquals(postBlocks.get(i).getPred().getLocalId(), postBlocks.get(j).getPred().getLocalId());
                    }
                }

            }
        }
    }

    @Test
    void testConsideringOfNeighbouringBlocks() {
        int postId = 33076987;
        PostVersionList q_33076987 = PostVersionList.readFromCSV(pathToPostHistory, postId, 1, false);
        q_33076987.processVersionHistory(PostVersionHistoryTest.configEqual);

        PostVersion version_2 = q_33076987.get(1);
        List<PostBlockVersion> version_2_postBlocks =  version_2.getPostBlocks();

        assertNull(version_2_postBlocks.get(0).getPred());

        assertNull(version_2_postBlocks.get(1).getPred());

        assertNotNull(version_2_postBlocks.get(2).getPred());
        assertEquals(Integer.valueOf(5), version_2_postBlocks.get(2).getPred().getLocalId());

        assertNotNull(version_2_postBlocks.get(3).getPred());
        assertEquals(Integer.valueOf(6), version_2_postBlocks.get(3).getPred().getLocalId());

        assertNotNull(version_2_postBlocks.get(4).getPred());
        assertEquals(Integer.valueOf(7), version_2_postBlocks.get(4).getPred().getLocalId());

        assertNull(version_2_postBlocks.get(5).getPred());
    }
}
