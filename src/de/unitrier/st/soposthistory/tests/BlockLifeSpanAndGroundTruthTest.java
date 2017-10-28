package de.unitrier.st.soposthistory.tests;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.gt.PostBlockConnection;
import de.unitrier.st.soposthistory.gt.PostGroundTruth;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpan;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpanVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockLifeSpanAndGroundTruthTest {
    private static Path pathToHistory = Paths.get("testdata");
    private static Path pathToGT = Paths.get("testdata", "gt");

    @Test
    void testReadFromDirectory() {
        List<PostGroundTruth> postGroundTruthList = PostGroundTruth.readFromDirectory(pathToGT);
        try {
            assertEquals(Files.list(pathToGT).filter(
                    file -> file
                            .getFileName()
                            .toString()
                            .endsWith(".csv"))
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
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToHistory, 22037280, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGT, 22037280);

        List<PostBlockLifeSpan> lifeSpans = a_22037280.extractPostBlockLifeSpans();
        List<PostBlockLifeSpan> lifeSpansGT = a_22037280_gt.extractPostBlockLifeSpans();
        assertEquals(lifeSpans.size(), lifeSpansGT.size());
        assertEquals(5, lifeSpans.size());
        for (int i=0; i<lifeSpans.size(); i++) {
            assertTrue(lifeSpans.get(i).equals(lifeSpansGT.get(i)));
        }
    }

    @Test
    void testPostBlockLifeSpanExtractionFilter() {
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToHistory, 22037280, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGT, 22037280);

        // text
        List<PostBlockLifeSpan> textBlockLifeSpans = a_22037280.extractPostBlockLifeSpans(
                Sets.newHashSet(TextBlockVersion.postBlockTypeId));
        List<PostBlockLifeSpan> textLifeSpansGT = a_22037280_gt.extractPostBlockLifeSpans(
                Sets.newHashSet(TextBlockVersion.postBlockTypeId)
        );
        assertEquals(textBlockLifeSpans.size(), textLifeSpansGT.size());
        assertEquals(3, textBlockLifeSpans.size());
        for (int i=0; i<textBlockLifeSpans.size(); i++) {
            assertTrue(textBlockLifeSpans.get(i).equals(textLifeSpansGT.get(i)));
        }

        // code
        List<PostBlockLifeSpan> codeBlockLifeSpans = a_22037280.extractPostBlockLifeSpans(
                Sets.newHashSet(CodeBlockVersion.postBlockTypeId));
        List<PostBlockLifeSpan> codeLifeSpansGT = a_22037280_gt.extractPostBlockLifeSpans(
                Sets.newHashSet(CodeBlockVersion.postBlockTypeId)
        );
        assertEquals(codeBlockLifeSpans.size(), codeLifeSpansGT.size());
        assertEquals(2, codeBlockLifeSpans.size());
        for (int i=0; i<codeBlockLifeSpans.size(); i++) {
            assertTrue(codeBlockLifeSpans.get(i).equals(codeLifeSpansGT.get(i)));
        }

        // TODO: Lorik: assertEquals(6, connectionsOfAllVersionsGroundTruth_text.size());???
    }

    @Test
    void testPostBlockConnectionExtraction() {
        PostVersionList a_22037280 = PostVersionList.readFromCSV(pathToHistory, 22037280, 2);
        PostGroundTruth a_22037280_gt = PostGroundTruth.readFromCSV(pathToGT, 22037280);

        List<PostBlockLifeSpan> lifeSpans = a_22037280.extractPostBlockLifeSpans();
        List<PostBlockLifeSpan> lifeSpansGT = a_22037280_gt.extractPostBlockLifeSpans();

        Set<PostBlockConnection> connections = PostBlockConnection.extractFromPostBlockLifeSpan(lifeSpans);
        Set<PostBlockConnection> connectionsGT = PostBlockConnection.extractFromPostBlockLifeSpan(lifeSpansGT);

        assertEquals(connections.size(), connectionsGT.size());
        assertTrue(PostBlockConnection.equals(connections, connectionsGT));
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

//    @Test
//    void testNumberOfPredecessorsOfOnePost() {
//        int postId = 3758880;
//
//        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToCSVs);
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
//        for (ConnectionsOfAllVersions connectionsOfAllVersions : groundTruthExtractionOfCSVs.getGroundTruth()) {
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
