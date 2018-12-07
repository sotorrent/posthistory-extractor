import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockSimilarity;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.history.PostHistory;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;
import org.sotorrent.posthistoryextractor.version.TitleVersion;
import org.sotorrent.posthistoryextractor.version.TitleVersionList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sotorrent.util.exceptions.InputTooShortException;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OtherPostHistoryTest {

    @Test
    void testRootPostBlockVersionIdAnswer3758880() {
        PostVersionList a_3758880 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3758880, Posts.ANSWER_ID);

        // there are 11 versions of this post
        assertEquals(11, a_3758880.size());

        PostVersion version_1 = a_3758880.get(0);

        assertEquals(6, version_1.getPostBlocks().size());
        // root post blocks of first version must be null
        for (PostBlockVersion currentPostBlock : version_1.getPostBlocks()) {
            assertEquals(currentPostBlock.getId(), (int) currentPostBlock.getRootPostBlockVersionId());
        }

        PostVersion version_2 = a_3758880.get(1);
        TestUtils.testPredecessorSimilarities(version_2);

        assertEquals(4, version_2.getPostBlocks().size());
        // first code block of second version has first code block of first version as root post block
        Assertions.assertEquals((int) version_2.getCodeBlocks().get(0).getRootPostBlockVersionId(),
                version_1.getCodeBlocks().get(0).getId());
        // second code block of second version has third code block of first version as root post block
        Assertions.assertEquals((int) version_2.getCodeBlocks().get(1).getRootPostBlockVersionId(),
                version_1.getCodeBlocks().get(2).getId());
        // second text block of second version has no predecessor (-> itself as root post block)
        Assertions.assertEquals((int) version_2.getTextBlocks().get(0).getRootPostBlockVersionId(),
                version_2.getTextBlocks().get(0).getId());

        PostVersion lastPostVersion = a_3758880.get(a_3758880.size() - 1);
        TestUtils.testPredecessorSimilarities(lastPostVersion);

        // first code block of last version still has first code block of first version as root post block
        Assertions.assertEquals((int) lastPostVersion.getCodeBlocks().get(0).getRootPostBlockVersionId(),
                version_1.getCodeBlocks().get(0).getId());
        // first text block of last version has first text block of second version as root post block
        Assertions.assertEquals((int) lastPostVersion.getTextBlocks().get(0).getRootPostBlockVersionId(),
                version_2.getTextBlocks().get(0).getId());
    }

    @Test
    void testVersionOrderQuestion3381751() {
        PostVersionList q_3381751 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3381751, Posts.QUESTION_ID);

        PostVersion previousVersion = q_3381751.get(0);
        for (int i = 1; i < q_3381751.size(); i++) {
            PostVersion currentVersion = q_3381751.get(i);
            assertTrue(currentVersion.getPostHistoryId() > previousVersion.getPostHistoryId());
        }
    }

    @Test
    void testPostBlockTypeFilter3758880() {
        PostVersionList q_3758880 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3758880, Posts.QUESTION_ID);
        // This caused a null pointer exception before (last commit: d37e6e38c8c15efe743e35141561742d7ef91ede),
        // because some filter checks were missing.
        q_3758880.processVersionHistory(CodeBlockVersion.getPostBlockTypeIdFilter());
    }

    @Test
    void testConfiguration() {
        // text
        assertThrows(IllegalArgumentException.class, () -> Config.EMPTY.withTextSimilarityThreshold(-1.0));
        assertThrows(IllegalArgumentException.class, () -> Config.EMPTY.withTextSimilarityThreshold(+1.1));

        // code
        assertThrows(IllegalArgumentException.class, () -> Config.EMPTY.withCodeSimilarityThreshold(-1.0));
        assertThrows(IllegalArgumentException.class, () -> Config.EMPTY.withCodeSimilarityThreshold(+1.1));
    }

    @Test
    void testBackupMetric() {
        // text blocks
        TextBlockVersion textBlock1 = new TextBlockVersion(1, 1);
        textBlock1.setContent("ab");
        TextBlockVersion textBlock2 = new TextBlockVersion(2, 2);
        textBlock2.setContent("ac");

        textBlock1.compareTo(textBlock2, Config.DEFAULT); // no exception
        assertThrows(InputTooShortException.class, () -> textBlock1.compareTo(textBlock2,
                Config.DEFAULT.withTextBackupSimilarityMetric(null))
        );
        PostBlockSimilarity similarity = textBlock1.compareTo(textBlock2, Config.EMPTY);
        assertEquals(0.0, similarity.getMetricResult());

        // code blocks
        CodeBlockVersion codeBlock1 = new CodeBlockVersion(1, 1);
        codeBlock1.setContent("foo");
        CodeBlockVersion codeBlock2 = new CodeBlockVersion(2, 2);
        codeBlock2.setContent("bar");

        codeBlock1.compareTo(codeBlock2, Config.DEFAULT); // no exception
        // commented out, because new default code metric "tokenDiceNormalized" doesn't need a backup metric
        //assertThrows(InputTooShortException.class, () -> codeBlock1.compareTo(codeBlock2,
        //        Config.DEFAULT.withCodeBackupSimilarityMetric(null))
        //);
        similarity = codeBlock1.compareTo(codeBlock2, Config.EMPTY);
        assertEquals(0.0, similarity.getMetricResult());
    }

    @Test
    void testReadFromDirectory() {
        List<PostVersionList> postVersionList = PostVersionList.readFromDirectory(TestUtils.pathToPostVersionLists);
        try {
            assertEquals(Files.list(TestUtils.pathToPostVersionLists).filter(
                    file -> PostHistory.fileNamePattern.matcher(file.toFile().getName()).matches())
                            .count(),
                    postVersionList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testReset() {
        PostVersionList a_3758880 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3758880, Posts.ANSWER_ID, false);

        Config config = Config.DEFAULT
                .withTextSimilarityMetric(org.sotorrent.stringsimilarity.set.Variants::fourGramOverlap)
                .withTextSimilarityThreshold(0.6)
                .withTextBackupSimilarityMetric(org.sotorrent.stringsimilarity.edit.Variants::levenshtein)
                .withTextBackupSimilarityThreshold(0.6)
                .withCodeSimilarityMetric(org.sotorrent.stringsimilarity.set.Variants::fourGramOverlap)
                .withCodeSimilarityThreshold(0.6)
                .withCodeBackupSimilarityMetric(org.sotorrent.stringsimilarity.edit.Variants::levenshtein)
                .withCodeBackupSimilarityThreshold(0.6);

        // there are 11 versions of this post
        assertEquals(11, a_3758880.size());

        // but the post blocks don't have predecessors yet, thus each block has its own lifespan
        assertEquals(47, a_3758880.getPostBlockVersionCount());
        assertEquals(a_3758880.getPostBlockVersionCount(), a_3758880.getPostBlockLifeSpans().size());

        a_3758880.processVersionHistory(config);

        // the post blocks have been extracted
        assertEquals(10, a_3758880.getPostBlockLifeSpans().size());

        // resetPostBlockVersionHistory version list
        a_3758880.resetPostBlockVersionHistory();

        // there are still 11 versions of this post
        assertEquals(11, a_3758880.size());

        // but the post blocks again don't have predecessors, thus each block has its own lifespan
        assertEquals(47, a_3758880.getPostBlockVersionCount());
        assertEquals(a_3758880.getPostBlockVersionCount(), a_3758880.getPostBlockLifeSpans().size());
    }

    @Test
    void testInvalidCharBetweenEncapsulatedTokenAndDelimiter() {
        // this post produced an IOException due to wrong escaping of backslashes in combination with double quotes
        PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 10049438, Posts.ANSWER_ID, false);
    }

    @Test
    void testLocalIdAssignment(){
        // local ids were previously not updated in PostHistory.reviseAndFinalizePostBlocks after merging of post blocks

        PostVersionList a_33058542 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 33058542, Posts.ANSWER_ID, true);

        PostVersion version_2 = a_33058542.get(1);

        Assertions.assertEquals(1, version_2.getPostBlocks().get(0).getLocalId().intValue());
        Assertions.assertEquals(2, version_2.getPostBlocks().get(1).getLocalId().intValue());
        Assertions.assertEquals(3, version_2.getPostBlocks().get(2).getLocalId().intValue());
    }

    @Test
    void testEqualityBasedMetric() {
        // test if connections are set correctly when an equality-based metric is used

        PostVersionList q_19612096 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 19612096, Posts.QUESTION_ID, false);
        q_19612096.processVersionHistory(TestUtils.configEqual);

        PostVersion version_10 = q_19612096.getPostVersion(50536699);

        PostBlockVersion postBlock5 = version_10.getPostBlocks().get(4); // "Result in:"
        PostBlockVersion postBlock9 = version_10.getPostBlocks().get(8); // "Result in:"
        PostBlockVersion postBlock13 = version_10.getPostBlocks().get(12); // "Result in:"

        assertNull(postBlock5.getPred());

        assertNotNull(postBlock9.getPred());
        assertEquals(9, postBlock9.getPred().getLocalId().intValue());

        assertNotNull(postBlock13.getPred());
        assertEquals(13, postBlock13.getPred().getLocalId().intValue());
    }

    @Test
    void testEquals() {
        int postId = 10381975;
        PostVersionList q_10381975 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID, false);
        q_10381975.processVersionHistory(TestUtils.configEqual);

        PostVersion version_2 = q_10381975.getPostVersion(23853971);

        // post block with localId 12 is equal to post block with localId 8 and localId 12 in previous version
        // it should be matched with the latter (smaller localId difference)
        PostBlockVersion postBlock12 = version_2.getPostBlocks().get(11);
        assertNotNull(postBlock12.getPred());
        assertEquals(Integer.valueOf(12), postBlock12.getPred().getLocalId());
    }

    @Test
    void testEqualsContextAnswer37196630() {
        int postId = 37196630;
        PostVersionList a_37196630 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID, false);
        a_37196630.normalizeLinks();
        a_37196630.processVersionHistory(TestUtils.configEqual);

        PostVersion version_3 = a_37196630.getPostVersion(117953545);

        // text block "which gives:" is present several times (version 1: localId 3; version 2: localId 3+7)
        // correct match using context: localId 7
        PostBlockVersion postBlock3 = version_3.getPostBlocks().get(2);
        assertNull(postBlock3.getPred());

        PostBlockVersion postBlock7 = version_3.getPostBlocks().get(6);
        assertNotNull(postBlock7.getPred());
        assertEquals(Integer.valueOf(3), postBlock7.getPred().getLocalId());
    }

    @Test
    void testOrderOfPostHistory() {
        int postId = 1669;
        PostVersionList q_1669 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(10, q_1669.size());

        // assert that versions are ordered chronologically
        for (int i = 1; i < q_1669.size(); i++) {
            PostVersion currentPostVersion = q_1669.get(i);
            PostVersion previousPostVersion = q_1669.get(i-1);
            assertTrue(currentPostVersion.getCreationDate().after(previousPostVersion.getCreationDate()));
        }
    }

    @Test
    void testTitleHistoryQuestion309424() {
        int postId = 309424;
        TitleVersionList q_309424 = TitleVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId);
        assertEquals(5, q_309424.size());

        TitleVersion currentVersion;

        // first version
        currentVersion = q_309424.getFirst();
        assertNull(currentVersion.getPred());
        assertNotNull(currentVersion.getSucc());
        assertNotNull(currentVersion.getSuccEditDistance());
        assertNotNull(currentVersion.getSuccPostHistoryId());

        for (int i=1; i<q_309424.size()-1; i++) {
            currentVersion = q_309424.get(i);
            assertNotNull(currentVersion.getPred());
            assertNotNull(currentVersion.getPredEditDistance());
            assertNotNull(currentVersion.getPredPostHistoryId());
            assertNotNull(currentVersion.getSucc());
            assertNotNull(currentVersion.getSuccEditDistance());
            assertNotNull(currentVersion.getSuccPostHistoryId());
        }

        // last version
        currentVersion = q_309424.getLast();
        assertNotNull(currentVersion.getPred());
        assertNotNull(currentVersion.getPredEditDistance());
        assertNotNull(currentVersion.getPredPostHistoryId());
        assertNull(currentVersion.getSucc());
    }

    @Test
    void testPostHistoryTransformation() {
        int postId = 309424;

        PostVersionList q_309424_content = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(4, q_309424_content.size());
        for (PostVersion postVersion : q_309424_content) {
            assertTrue(PostHistory.contentPostHistoryTypes.contains(postVersion.getPostHistoryTypeId()));
        }

        TitleVersionList q_309424_title = TitleVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId);
        assertEquals(5, q_309424_title.size());
        for (TitleVersion titleVersion : q_309424_title) {
            assertTrue(PostHistory.titlePostHistoryTypes.contains(titleVersion.getPostHistoryTypeId()));
        }
    }

    @Test
    void testEmptyVersionList() {
        PostVersionList postVersionList = new PostVersionList(1, Posts.QUESTION_ID);
        postVersionList.sort(); // this should not throw an exception

        TitleVersionList titleVersionList = new TitleVersionList(1, Posts.QUESTION_ID);
        titleVersionList.sort(); // this should not throw an exception
    }

    @Test
    void testTitleVersionWrongPostType() {
        assertThrows(IllegalArgumentException.class, () -> new TitleVersion(1, 1, Posts.ANSWER_ID,
                (byte) 1, new Timestamp(0), "Title")
        );
    }

    @Test
    void testEmptyPostVersionQuestion29813692() {
        // this post has content on the SO website (https://stackoverflow.com/q/29813692),
        // but not in the post history (https://stackoverflow.com/posts/29813692/revisions)
        int postId = 29813692;
        PostVersionList q_29813692 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(0, q_29813692.size());
    }

    @Test
    void testLastTextBlockReferenceLink() {
        PostVersionList q_41480290 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 41480290, Posts.QUESTION_ID, true);

        // last text block in that version only contains a reference link
        PostVersion version_2 = q_41480290.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 3, 2, 1);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        // after link normalization, that text block is gone
        q_41480290.normalizeLinks();
        version_2 = q_41480290.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 2, 1, 1);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);
    }
}
