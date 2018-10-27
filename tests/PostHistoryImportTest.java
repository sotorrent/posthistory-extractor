import org.junit.jupiter.api.Test;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostHistoryImportTest {
    @Test
    void testReadPostHistoryAnswer1109108() {
        PostVersionList a_1109108 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 1109108, Posts.ANSWER_ID);

        assertEquals(7, a_1109108.size());

        PostVersion version_7 = a_1109108.get(6);
        TestUtils.testPredecessorSimilarities(version_7);

        TestUtils.testPostBlockCount(version_7, 3, 2, 1);

        CodeBlockVersion codeBlock_1 = version_7.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(6, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }

        TextBlockVersion textBlock_1 = version_7.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(1, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
    }

    @Test
    void testReadPostHistoryAnswer3145655() {
        PostVersionList a_3145655 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3145655, Posts.ANSWER_ID);

        assertEquals(7, a_3145655.size());

        PostVersion version_7 = a_3145655.get(6);
        TestUtils.testPredecessorSimilarities(version_7);

        TestUtils.testPostBlockCount(version_7, 5,3, 2);

        CodeBlockVersion codeBlock_1 = version_7.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(8, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }

        TextBlockVersion textBlock_1 = version_7.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(7, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
    }

    @Test
    void testReadPostHistoryAnswer9855338() {
        PostVersionList a_9855338 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 9855338, Posts.ANSWER_ID);

        assertEquals(11, a_9855338.size());

        PostVersion version_11 = a_9855338.get(10);
        TestUtils.testPredecessorSimilarities(version_11);

        TestUtils.testPostBlockCount(version_11, 3, 2, 1);

        CodeBlockVersion codeBlock_1 = version_11.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(10, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }

        TextBlockVersion textBlock_1 = version_11.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(1, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
    }

    @Test
    void testReadPostHistoryAnswer2581754() {
        PostVersionList a_2581754 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 2581754, Posts.ANSWER_ID);

        assertEquals(8, a_2581754.size());

        PostVersion version_3 = a_2581754.get(2);
        TestUtils.testPredecessorSimilarities(version_3);

        TestUtils.testPostBlockCount(version_3, 6, 3, 3);

        CodeBlockVersion codeBlock_1 = version_3.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(25, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }

        TextBlockVersion textBlock_1 = version_3.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(1, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
    }

    @Test
    void testReadPostHistoryAnswer20991163() {
        PostVersionList a_20991163 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 20991163, Posts.ANSWER_ID);
        // this post should only consist of one code block (not an empty text block at the end)
        assertEquals(1, a_20991163.size());
        PostVersion version_1 = a_20991163.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 0, 1);
    }

    @Test
    void testReadPostHistoryAnswer32012927() {
        PostVersionList a_32012927 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 32012927, Posts.ANSWER_ID);
        assertEquals(4, a_32012927.size());
        // the first version of this post should only consist of one text block
        PostVersion version_1 = a_32012927.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
    }

    @Test
    void testReadPostHistoryAnswer10734905() {
        PostVersionList a_10734905 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 10734905, Posts.ANSWER_ID);

        assertEquals(1, a_10734905.size());

        // the first and only version of this post should consist of three text blocks and two code blocks
        PostVersion version_1 = a_10734905.get(0);
        TestUtils.testPostBlockCount(version_1, 5, 3, 2);
        TestUtils.testPostBlockTypes(version_1, TextBlockVersion.class);
    }

    @Test
    void testReadPostHistoryAnswer31965641() {
        PostVersionList a_31965641 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 31965641, Posts.ANSWER_ID);

        assertEquals(1, a_31965641.size());

        // the first and only version of this post should consist of two text blocks and two code blocks
        PostVersion version_1 = a_31965641.get(0);
        TestUtils.testPostBlockCount(version_1, 4, 2, 2);
        TestUtils.testPostBlockTypes(version_1,TextBlockVersion.class);
    }

    @Test
    void testReadPostHistoryQuestion22360443() {
        PostVersionList q_22360443 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 22360443, Posts.QUESTION_ID);

        assertEquals(2, q_22360443.size());

        PostVersion version_1 = q_22360443.get(0);
        TestUtils.testPostBlockCount(version_1, 4, 2, 2);
        TestUtils.testPostBlockTypes(version_1, CodeBlockVersion.class);

        PostVersion version_2 = q_22360443.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 5, 3, 2);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);
    }

    @Test
    void testReadPostHistoryQuestion47555767() {
        int postId = 47555767;

        PostVersionList q_47555767 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(1, q_47555767.size());

        PostVersion version_1 = q_47555767.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
        TextBlockVersion textBlock = version_1.getTextBlocks().get(0);
        assertTrue(textBlock.getContent().trim().length() > 0);
    }

    @Test
    void testReadPostHistoryAnswer45204073() {
        int postId = 45204073;

        PostVersionList a_45204073 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(1, a_45204073.size());

        PostVersion version_1 = a_45204073.get(0);
        TestUtils.testPostBlockCount(version_1, 2, 1, 1);
    }

    @Test
    void testReadPostHistoryAnswer2376203() {
        int postId = 2376203;

        PostVersionList a_2376203 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(1, a_2376203.size());

        PostVersion version_1 = a_2376203.get(0);
        // should have one TextBlock with content "......", but this is merged according to our heuristic
        TestUtils.testPostBlockCount(version_1, 1, 0, 1);
    }

    @Test
    void testReadPostHistoryAnswer45163319() {
        int postId = 45163319;

        PostVersionList a_45163319 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(1, a_45163319.size());

        PostVersion version_1 = a_45163319.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
    }

    @Test
    void testReadPostHistoryQuestion1257964() {
        int postId = 1257964;

        PostVersionList q_1257964 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(5, q_1257964.size());

        // content of this version contains only whitespace ("  &#xD;&#xA;   ")
        PostVersion version_3 = q_1257964.getPostVersion(2575481);
        TestUtils.testPostBlockCount(version_3, 0, 0, 0);
    }

    @Test
    void testReadPostHistoryInlineStackSnippet() {
        int postId = 26365857;
        PostVersionList q_26365857 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(3, q_26365857.size());

        // this version contains inline stack snippets with language information -> considered part of text block
        PostVersion version_2 = q_26365857.getPostVersion(75518502);
        TestUtils.testPostBlockCount(version_2, 1, 1, 0);

        postId = 27994382;
        PostVersionList a_27994382 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(2, a_27994382.size());

        // this version contains inline stack snippets with language information -> considered part of text block
        PostVersion version_1 = a_27994382.getPostVersion(81771332);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
    }

    @Test
    void testReadPostHistoryWithEmptyVersion() {
        int postId = 1450250;
        PostVersionList q_1450250 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        assertEquals(2, q_1450250.size());

        // content of this version is empty
        PostVersion version_1 = q_1450250.getPostVersion(2870161);
        TestUtils.testPostBlockCount(version_1, 0, 0, 0);

        postId = 1223598;
        PostVersionList a_1223598 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(2, a_1223598.size());

        // content of this version is empty
        version_1 = a_1223598.getPostVersion(2398799);
        TestUtils.testPostBlockCount(version_1, 0, 0, 0);
    }

    @Test
    void testReadPostHistoryEmptyCodeBlock() {
        // the following posts were removed from the test data for test case testPostVersionsWithoutContent (disabled),
        // because they have non-whitespace content, but should still be treated as having no post blocks
        // (i.e., they only contain an empty code block)

        int postId = 5864258;
        PostVersionList a_5864258 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(2, a_5864258.size());

        // content of this version is an empty code block, which is ignored
        PostVersion version_1 = a_5864258.getPostVersion(12646646);
        TestUtils.testPostBlockCount(version_1, 0, 0, 0);

        postId = 9875710;
        PostVersionList a_9875710 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(3, a_9875710.size());

        // content of this version is an empty code block, which is ignored
        version_1 = a_9875710.getPostVersion(22583841);
        TestUtils.testPostBlockCount(version_1, 0, 0, 0);
    }

    @Test
    void testReadPostHistoryAnswer28280446() {
        // this post was present in SO dump 2017-12-01, but not anymore in 2018-03-13
        int postId = 28280446;
        PostVersionList a_28280446 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        assertEquals(1, a_28280446.size());

        PostVersion version_1 = a_28280446.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
    }
}
