package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.gt.GroundTruth;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpan;
import de.unitrier.st.soposthistory.gt.PostBlockLifeSpanVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockLifeSpanAndGroundTruthTest {
    private static Path pathToHistory = Paths.get("testdata");
    private static Path pathToGT = Paths.get("testdata", "gt");

    @Test
    void testReadFromDirectory() {
        List<GroundTruth> groundTruthList = GroundTruth.readFromDirectory(pathToGT);
        try {
            assertEquals(Files.list(pathToGT).filter(
                    file -> file
                            .getFileName()
                            .toString()
                            .endsWith(".csv"))
                            .count(),
                    groundTruthList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testPostBlockLifeSpanVersionsEqual(){
        // compare two PostBlockLifeSpanVersions
        PostBlockLifeSpanVersion original = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 0, "", 1);
        PostBlockLifeSpanVersion differentPostId = new PostBlockLifeSpanVersion(4712, 42, 1, 0, 0, 0, "", 1);
        PostBlockLifeSpanVersion differentPostHistoryId = new PostBlockLifeSpanVersion(4711, 43, 1, 0, 0, 0, "", 1);
        PostBlockLifeSpanVersion differentPostBlockTypeId = new PostBlockLifeSpanVersion(4711, 42, 2, 0, 0, 0, "", 1);
        PostBlockLifeSpanVersion differentVersion = new PostBlockLifeSpanVersion(4711, 42, 1, 1, 0, 0, "", 2);
        PostBlockLifeSpanVersion differentLocalId = new PostBlockLifeSpanVersion(4711, 42, 1, 1, 0, 0, "", 1);
        PostBlockLifeSpanVersion differentPredId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 1, 0,"", 1);
        PostBlockLifeSpanVersion differentSuccId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 1, "", 1);

        assertThat(original, is(original));
        assertThat(original, is(not(differentPostId)));
        assertThat(original, is(differentPostHistoryId));
        assertThat(original, is(not(differentPostBlockTypeId)));
        assertThat(original, is(not(differentVersion)));
        assertThat(original, is(not(differentLocalId)));
        assertThat(original, is(differentPredId));
        assertThat(original, is(differentSuccId));
    }

    @Test
    void testPostBlockLifeSpanExtraction() {
        PostVersionList a_3758880 = PostVersionList.readFromCSV(pathToHistory, 22037280, 2);
        List<PostBlockLifeSpan> lifeSpans = a_3758880.extractPostBlockLifeSpans();

        GroundTruth a_22037280_gt = GroundTruth.readFromCSV(pathToGT, 22037280);
        List<PostBlockLifeSpan> lifeSpansGT = a_22037280_gt.extractPostBlockLifeSpans();

        assertEquals(lifeSpans.size(), lifeSpansGT.size());

        for (int i=0; i<lifeSpans.size(); i++) {
            PostBlockLifeSpan currentLifeSpan = lifeSpans.get(i);
            PostBlockLifeSpan currentLifeSpanGT = lifeSpansGT.get(i);

            assertEquals(currentLifeSpanGT.size(), currentLifeSpan.size());

            for (int j=0; j<currentLifeSpan.size(); j++) {
                PostBlockLifeSpanVersion currentLifeSpanVersion = currentLifeSpan.get(j);
                PostBlockLifeSpanVersion currentLifeSpanVersionGT = currentLifeSpanGT.get(j);

                assertEquals(currentLifeSpanVersionGT, currentLifeSpanVersion);
            }
        }
    }


//    @Test
//    void testGroundTruthExtractionOfCSV(){
//        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(Paths.get("testdata", "Samples_test", "fewCompletedFiles").toString());
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
//    void testExtractionsForOnePost() {
//
//        int postId = 22037280;
//
//        GroundTruthExtractionOfCSVs groundTruthExtractionOfCSVs = new GroundTruthExtractionOfCSVs(pathToFewCompletedFiles);
//        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_text = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_text(postId);
//        ConnectionsOfAllVersions connectionsOfAllVersionsGroundTruth_code = groundTruthExtractionOfCSVs.getAllConnectionsOfAllConsecutiveVersions_code(postId);
//
//        PostVersionsListManagement postVersionsListManagement = new PostVersionsListManagement(pathToFewCompletedFiles);
//        postVersionsListManagement.getPostVersionListWithID(postId).processVersionHistory();
//        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_text = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_text(postId);
//        ConnectionsOfAllVersions connectionsOfAllVersionsComputedMetric_code = postVersionsListManagement.getAllConnectionsOfAllConsecutiveVersions_code(postId);
//
//
//        assertEquals(6, connectionsOfAllVersionsGroundTruth_text.size());
//
//        assertThat(connectionsOfAllVersionsComputedMetric_text, is(connectionsOfAllVersionsComputedMetric_text));
//        assertThat(connectionsOfAllVersionsComputedMetric_code, is(connectionsOfAllVersionsComputedMetric_code));
//
//    }
//
//
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
