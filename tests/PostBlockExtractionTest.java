import org.junit.jupiter.api.Test;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostBlockExtractionTest {

    @Test
    void testStackSnippetCodeBlocksAnswer32143330() {
        PostVersionList a_32143330 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 32143330, Posts.ANSWER_ID);

        assertEquals(4, a_32143330.size());

        // Test if Stack Snippets (see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/)
        // and snippet language information blocks (see https://stackoverflow.com/editing-help#syntax-highlighting)
        // are correctly handled (language info splits code blocks).
        PostVersion version_4 = a_32143330.get(3);
        TestUtils.testPredecessorSimilarities(version_4);
        TestUtils.testPostBlockCount(version_4, 6, 3, 3);
        TestUtils.testPostBlockTypes(version_4, new Class[]{
                TextBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class,
                CodeBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class});
    }

    @Test
    void testStackSnippetCodeBlocksAnswer26044128() {
        PostVersionList a_26044128 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 26044128, Posts.ANSWER_ID);

        assertEquals(12, a_26044128.size());

        PostVersion version_12 = a_26044128.get(11);
        TestUtils.testPredecessorSimilarities(version_12);
        TestUtils.testPostBlockCount(version_12, 8, 4, 4);
        TestUtils.testPostBlockTypes(version_12, new Class[]{
                TextBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class,
                CodeBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class}); // Markdown links
    }

    @Test
    void testAlternativeCodeBlockQuestion32342082() {
        PostVersionList q_32342082 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 32342082, Posts.QUESTION_ID);

        assertEquals(8, q_32342082.size());

        // version 5 contains an alternative (GitHub-style) code block (see https://stackoverflow.com/revisions/32342082/5)
        PostVersion version_5 = q_32342082.get(4);
        TestUtils.testPredecessorSimilarities(version_5);
        TestUtils.testPostBlockCount(version_5, 5, 3, 2);
        TestUtils.testPostBlockTypes(version_5, TextBlockVersion.class);
    }

    @Test
    void testCodeTagCodeBlockQuestion19175014() {
        PostVersionList q_19175014 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 19175014, Posts.QUESTION_ID);

        assertEquals(2, q_19175014.size());

        // version 1+2 contain a code block marked by <pre><code> ... </pre></code> instead of correct indention (see https://stackoverflow.com/revisions/19175014/2)
        PostVersion version_2 = q_19175014.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 2, 1, 1);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);
    }

    @Test
    void testScriptTagCodeBlockQuestion3381751() {
        PostVersionList q_3381751 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3381751, Posts.QUESTION_ID);

        assertEquals(15, q_3381751.size());

        // version 1 contains a code block marked by <script type="text/javascript"> ... </script> instead of correct indention (see https://stackoverflow.com/revisions/3381751/1)
        PostVersion version_1 = q_3381751.get(0);
        TestUtils.testPostBlockCount(version_1, 3, 2, 1);
        TestUtils.testPostBlockTypes(version_1, TextBlockVersion.class);
    }

    @Test
    void testScriptTagInIndentedCodeBlockQuestion28598648() {
        PostVersionList q_28598648 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 28598648, Posts.QUESTION_ID);

        assertEquals(2, q_28598648.size());

        // version 1+2 contain an indented code block containing <script> tags (see https://stackoverflow.com/revisions/28598648/2)
        PostVersion version_2 = q_28598648.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 2, 1, 1);
        TestUtils.testPostBlockTypes(version_2, CodeBlockVersion.class);
    }

    @Test
    void testBrokenTextBlockQuestion15372744() {
        PostVersionList q_15372744 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 15372744, Posts.QUESTION_ID);

        // version 1 contains a broken text block, which has an indented line.
        // Stack Overflow displays this correctly (see https://stackoverflow.com/revisions/15372744/1)
        PostVersion version_1 = q_15372744.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
        List<PostBlockVersion> postBlocks = version_1.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
    }

    @Test
    void testSnippetDivider() {
        // in this post, an empty XML comment ("<!-- -->") is used to divide code blocks
        int postId = 33058542;
        PostVersionList a_33058542 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        PostVersion version_1 = a_33058542.get(0);
        TestUtils.testPostBlockCount(version_1, 5, 2, 3);
        PostVersion version_2 = a_33058542.get(1);
        TestUtils.testPostBlockCount(version_2, 5, 2, 3);

        // in the second version of this post, an empty XML processing instruction ("<?-- -->") is used to divide code blocks
        postId = 33845232;
        PostVersionList a_33845232 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        version_1 = a_33845232.get(0);
        TestUtils.testPostBlockCount(version_1, 1, 1, 0);
        version_2 = a_33845232.get(1);
        TestUtils.testPostBlockCount(version_2, 5, 1, 4);
    }

    @Test
    void testPredContextLastPostBlockAnswer32841902() {
        int postId = 32841902;
        PostVersionList a_10381975 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID, false);
        a_10381975.processVersionHistory(TestUtils.configEqual);

        PostVersion version_2 = a_10381975.getPostVersion(100687945);

        // the last two post blocks should be connected (version 1: localId 2+3; version 2: localId 5+6)
        PostBlockVersion postBlock5 = version_2.getPostBlocks().get(4);
        assertNotNull(postBlock5.getPred());
        assertEquals(Integer.valueOf(2), postBlock5.getPred().getLocalId());

        PostBlockVersion postBlock6 = version_2.getPostBlocks().get(5);
        assertNotNull(postBlock6.getPred());
        assertEquals(Integer.valueOf(3), postBlock6.getPred().getLocalId());
    }

    @Test
    void testPreWithoutCode() {
        // this post has code blocks enclosed by <pre> ... </pre> without a <code> tag
        int postId = 18932575;
        PostVersionList q_18932575 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);

        PostVersion version_1 = q_18932575.get(0);
        TestUtils.testPostBlockCount(version_1, 7, 4, 3);

        // the second and third code snippet start with a non-indented line that is nevertheless part of the code snippet
        CodeBlockVersion code_block_2 = version_1.getCodeBlocks().get(1);
        assertTrue(code_block_2.getContent().startsWith("> package "));
        CodeBlockVersion code_block_3 = version_1.getCodeBlocks().get(2);
        assertTrue(code_block_3.getContent().startsWith("> package "));
    }

    @Test
    void testQuestion49311839() {
        // this post uses inline-style code blocks + HTML newlines
        int postId = 49311839;
        PostVersionList q_49311839 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);

        PostVersion version_4 = q_49311839.get(3);
        TestUtils.testPostBlockCount(version_4, 7, 4, 3);
    }

    @Test
    void testQuestion46695379() {
        // this post had 291 post blocks due to indention in log files being treated as code blocks
        int postId = 46695379;
        PostVersionList q_46695379 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);

        PostVersion version_6 = q_46695379.get(5);
        TestUtils.testPostBlockCount(version_6, 3, 2, 1);
        List<PostBlockVersion> version_6_postblocks = version_6.getPostBlocks();
        assertTrue(version_6_postblocks.get(0).getContent().trim().startsWith("I am working on a project in django with pycharm"));
        assertTrue(version_6_postblocks.get(1).getContent().trim().startsWith("def all_songs(request, filter_by)"));
        assertTrue(version_6_postblocks.get(2).getContent().trim().startsWith("Everything is working fine when I click on the page that run this method, but I get an error"));
    }

    @Test
    void testAnswer29443655() {
        // this post has an "empty" code block only containing an HTMl iframe that is not rendered on the website.
        int postId = 29443655;
        PostVersionList a_29443655 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);

        PostVersion version_6 = a_29443655.get(5);
        TestUtils.testPostBlockCount(version_6, 15, 8, 7);
        List<PostBlockVersion> version_6_postblocks = version_6.getPostBlocks();
        assertEquals("<iframe width=\"100%\" height=\"300\" src=\"//jsfiddle.net/vyab4kcf/44/embedded/\" allowfullscreen=\"allowfullscreen\" frameborder=\"0\"></iframe>", version_6_postblocks.get(14).getContent().trim());
    }

    @Test
    void testQuestion28402792() {
        /* the first version of this post contains a <pre> tag with a class attribute, followed by a <code> tag
         * in a new line:
         * <pre class="lang-rb prettyprint prettyprinted">
         * <code>
         */
        int postId = 28402792;
        PostVersionList q_28402792 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);

        PostVersion version_1 = q_28402792.get(0);
        TestUtils.testPostBlockCount(version_1, 3, 2);

        // first code block
        assertEquals("[{\"sum\":{\"key1\":0,\"key2\":\"2014\",\"key3\":0,\"key4\":\"8\",\"key5\":0,\"key6\":\"0\",\"key7\":0}},{\"sum\":{\"key1\":0,\"key2\":\"2014\",\"key3\":0,\"key4\":\"12\",\"key5\":0,\"key6\":\"1\",\"key7\":0}}]",
                version_1.getCodeBlocks().get(0).getContent().trim()
        );

        // second code block
        assertEquals("[{\"key1\":0,\"key2\":\"2014\",\"key3\":0,\"key4\":\"8\",\"key5\":0,\"key6\":\"0\",\"key7\":0},{\"key1\":0,\"key2\":\"2014\",\"key3\":0,\"key4\":\"12\",\"key5\":0,\"key6\":\"1\",\"key7\":0}]",
                version_1.getCodeBlocks().get(1).getContent().trim()
        );
    }

    @Test
    void testAnswer31386254() {
        int postId = 31386254;
        PostVersionList a_31386254 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        PostVersion version_1 = a_31386254.get(0);

        // post contains one JavaScript stack snippet
        TestUtils.testPostBlockCount(version_1, 1, 1);
        assertEquals(1, version_1.getStackSnippets().size());
        TestUtils.testStackSnippets(version_1);
    }

    @Test
    void testAnswer54092960() {
        int postId = 54092960;
        PostVersionList a_54092960 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.ANSWER_ID);
        PostVersion version_1 = a_54092960.get(0);

        // post contains two CSV + HTML stack snippets plus text block after the last snippet
        TestUtils.testPostBlockCount(version_1, 3, 4);
        assertEquals(2, version_1.getStackSnippets().size());
        TestUtils.testStackSnippets(version_1);
    }

    @Test
    void testQuestion848841() {
        int postId = 848841;
        PostVersionList q_848841 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, postId, Posts.QUESTION_ID);
        PostVersion version_2 = q_848841.get(1);
        TestUtils.testPostBlockCount(version_2, 4, 4);

        // Test import of escaped newline characters
        assertEquals(
                // PostHistory.xml: style=&quot;width:&amp;#xD;&amp;#xA;           3&amp;#xD;&amp;#xA;     %;&quot;
                // Table PostHistory (parsed into MySQL): style="width:&#xD;&#xA;		3&#xD;&#xA;	%;"
                "style=\"width:&#xD;&#xA;3&#xD;&#xA;%;\"",
                version_2.getCodeBlocks().get(1).getContent().replaceAll("\\s", "")
        );
    }
}
