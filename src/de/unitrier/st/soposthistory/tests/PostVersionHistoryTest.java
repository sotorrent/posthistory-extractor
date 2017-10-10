package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PostVersionHistoryTest {

    @Test
    void testReadPostHistoryAnswer1109108() {
        PostVersionList a_1109108 = new PostVersionList();
        a_1109108.readFromCSV("testdata", 1109108, 2);

        assertEquals(7, a_1109108.size());

        PostVersion version_7 = a_1109108.get(6);
        assertEquals(3, version_7.getPostBlocks().size());
        assertEquals(2, version_7.getTextBlocks().size());
        assertEquals(1, version_7.getCodeBlocks().size());

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
        PostVersionList a_3145655 = new PostVersionList();
        a_3145655.readFromCSV("testdata", 3145655, 2);

        assertEquals(7, a_3145655.size());

        PostVersion version_7 = a_3145655.get(6);
        assertEquals(5, version_7.getPostBlocks().size());
        assertEquals(3, version_7.getTextBlocks().size());
        assertEquals(2, version_7.getCodeBlocks().size());

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
        PostVersionList a_9855338 = new PostVersionList();
        a_9855338.readFromCSV("testdata", 9855338, 2);

        assertEquals(11, a_9855338.size());

        PostVersion version_11 = a_9855338.get(10);
        assertEquals(3, version_11.getPostBlocks().size());
        assertEquals(2, version_11.getTextBlocks().size());
        assertEquals(1, version_11.getCodeBlocks().size());

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
        PostVersionList a_2581754 = new PostVersionList();
        a_2581754.readFromCSV("testdata", 2581754, 2);

        assertEquals(8, a_2581754.size());

        PostVersion version_3 = a_2581754.get(2);
        assertEquals(6, version_3.getPostBlocks().size());
        assertEquals(3, version_3.getTextBlocks().size());
        assertEquals(3, version_3.getCodeBlocks().size());

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
    void testPostBlockVersionExtraction() {
        // TODO: add test cases for testing the version history extraction (similarity and diffs between code blocks and text blocks, linking of block versions)

        //a_1109108.processVersionHistory();
        //PostVersion version_7 = a_1109108.get(6);
        //assertEquals(2, version_7.getCodeBlocks().get(0).getPredDiff().size());
        // ....
    }

    @Test
    void testReadPostHistoryAnswer20991163() {
        PostVersionList a_20991163 = new PostVersionList();
        a_20991163.readFromCSV("testdata", 20991163, 2);

        // this post should only consist of one code block (not an empty text block at the end)
        assertEquals(1, a_20991163.size());

        PostVersion version_1 = a_20991163.get(0);
        assertEquals(1, version_1.getPostBlocks().size());
        assertEquals(0, version_1.getTextBlocks().size());
        assertEquals(1, version_1.getCodeBlocks().size());
    }

    @Test
    void testReadPostHistoryAnswer32012927() {
        PostVersionList a_32012927 = new PostVersionList();
        a_32012927.readFromCSV("testdata", 32012927, 2);

        assertEquals(4, a_32012927.size());

        // the first version of this post should only consist of one text block
        PostVersion version_1 = a_32012927.get(0);
        assertEquals(1, version_1.getPostBlocks().size());
        assertEquals(1, version_1.getTextBlocks().size());
        assertEquals(0, version_1.getCodeBlocks().size());
    }

    @Test
    void testReadPostHistoryAnswer10734905() {
        PostVersionList a_10734905 = new PostVersionList();
        a_10734905.readFromCSV("testdata", 10734905, 2);

        assertEquals(1, a_10734905.size());

        // the first and only version of this post should consist of three text blocks and two code blocks
        PostVersion version_1 = a_10734905.get(0);
        assertEquals(5, version_1.getPostBlocks().size());
        assertEquals(3, version_1.getTextBlocks().size());
        assertEquals(2, version_1.getCodeBlocks().size());

        List<PostBlockVersion> postBlocks = version_1.getPostBlocks();

        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof TextBlockVersion);
    }

    @Test
    void testReadPostHistoryAnswer31965641() {
        PostVersionList a_31965641 = new PostVersionList();
        a_31965641.readFromCSV("testdata", 31965641, 2);

        assertEquals(1, a_31965641.size());

        // the first and only version of this post should consist of two text blocks and two code blocks
        PostVersion version_1 = a_31965641.get(0);
        assertEquals(4, version_1.getPostBlocks().size());
        assertEquals(2, version_1.getTextBlocks().size());
        assertEquals(2, version_1.getCodeBlocks().size());

        List<PostBlockVersion> postBlocks = version_1.getPostBlocks();

        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
    }

    @Test
    void testRootPostBlockVersionIdAnswer3758880() {
        PostVersionList a_3758880 = new PostVersionList();
        a_3758880.readFromCSV("testdata", 3758880, 2);

        // there are 11 versions of this post
        assertEquals(11, a_3758880.size());

        PostVersion firstVersion = a_3758880.get(0);
        assertEquals(6, firstVersion.getPostBlocks().size());
        // root post blocks of first version must be null
        for (PostBlockVersion currentPostBlock : firstVersion.getPostBlocks()) {
            assertEquals(currentPostBlock.getId(), (int)currentPostBlock.getRootPostBlockId());
        }

        PostVersion secondPostVersion = a_3758880.get(1);
        assertEquals(4, secondPostVersion.getPostBlocks().size());
        // first code block of second version has first code block of first version as root post block
        assertEquals((int)secondPostVersion.getCodeBlocks().get(0).getRootPostBlockId(),
                firstVersion.getCodeBlocks().get(0).getId());
        // second code block of second version has third code block of first version as root post block
        assertEquals((int)secondPostVersion.getCodeBlocks().get(1).getRootPostBlockId(),
                firstVersion.getCodeBlocks().get(2).getId());
        // second text block of second version has no predecessor (-> itself as root post block)
        assertEquals((int)secondPostVersion.getTextBlocks().get(0).getRootPostBlockId(),
                secondPostVersion.getTextBlocks().get(0).getId());

        PostVersion lastPostVersion = a_3758880.get(a_3758880.size()-1);
        // first code block of last version still has first code block of first version as root post block
        assertEquals((int)lastPostVersion.getCodeBlocks().get(0).getRootPostBlockId(),
                firstVersion.getCodeBlocks().get(0).getId());
        // first text block of last version has first text block of second version as root post block
        assertEquals((int)lastPostVersion.getTextBlocks().get(0).getRootPostBlockId(),
                secondPostVersion.getTextBlocks().get(0).getId());
    }

    @Test
    void testReadPostHistoryQuestion22360443() {
        PostVersionList q_22360443 = new PostVersionList();
        q_22360443.readFromCSV("testdata", 22360443, 1);

        assertEquals(2, q_22360443.size());

        PostVersion version_1 = q_22360443.get(0);
        assertEquals(4, version_1.getPostBlocks().size());
        assertEquals(2, version_1.getTextBlocks().size());
        assertEquals(2, version_1.getCodeBlocks().size());
        List<PostBlockVersion> postBlocks = version_1.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(1) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(2) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(3) instanceof TextBlockVersion);

        PostVersion version_2 = q_22360443.get(1);
        assertEquals(5, version_2.getPostBlocks().size());
        assertEquals(3, version_2.getTextBlocks().size());
        assertEquals(2, version_2.getCodeBlocks().size());
        postBlocks = version_2.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof TextBlockVersion);
    }

    @Test
    void testRootPostBlocksQuestion3758880() {
        PostVersionList q_3758880 = new PostVersionList();
        q_3758880.readFromCSV("testdata", 3758880, 1);

        assertEquals(11, q_3758880.size());

        // TODO: Implement way to test root post block assignment without connection to database
    }

    @Test
    void testStackSnippetCodeBlocksAnswer32143330() {
        PostVersionList a_32143330 = new PostVersionList();
        a_32143330.readFromCSV("testdata", 32143330, 2);

        assertEquals(4, a_32143330.size());

        // Test if Stack Snippets (see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/)
        // and snippet language information blocks (see https://stackoverflow.com/editing-help#syntax-highlighting)
        // are correctly handled (language info splits code blocks).
        PostVersion version_4 = a_32143330.get(3);
        assertEquals(6, version_4.getPostBlocks().size());
        assertEquals(3, version_4.getTextBlocks().size());
        assertEquals(3, version_4.getCodeBlocks().size());
        List<PostBlockVersion> postBlocks = version_4.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(5) instanceof TextBlockVersion);
    }

    @Test
    void testStackSnippetCodeBlocksAnswer26044128() {
        PostVersionList a_26044128 = new PostVersionList();
        a_26044128.readFromCSV("testdata", 26044128, 2);

        assertEquals(12, a_26044128.size());

        PostVersion version_12 = a_26044128.get(11);
        assertEquals(8, version_12.getPostBlocks().size());
        assertEquals(4, version_12.getTextBlocks().size());
        assertEquals(4, version_12.getCodeBlocks().size());
        List<PostBlockVersion> postBlocks = version_12.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(5) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(6) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(7) instanceof TextBlockVersion); // Markdown links
    }

    @Test
    void testAlternativeCodeBlockQuestion32342082() {
        PostVersionList q_32342082 = new PostVersionList();
        q_32342082.readFromCSV("testdata", 32342082, 1);

        assertEquals(8, q_32342082.size());

        // version 5 contains an alternative (GitHub-style) code block (see https://stackoverflow.com/revisions/32342082/5)
        PostVersion version_5 = q_32342082.get(4);
        assertEquals(5, version_5.getPostBlocks().size());
        assertEquals(3, version_5.getTextBlocks().size());
        assertEquals(2, version_5.getCodeBlocks().size());
        List<PostBlockVersion> postBlocks = version_5.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof TextBlockVersion);
    }

    @Test
    void testPredecessorAssignmentAnswer3758880(){
        // tests if posts blocks are set more than once as predecessor
        PostVersionList a_3758880 = new PostVersionList();
        a_3758880.readFromCSV("testdata", 3758880, 2, false);

        TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceVariant;
        a_3758880.processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);

        // we consider the most recent version here
        List<Integer> predecessorList = new LinkedList<>();
        List<TextBlockVersion> textBlocks = a_3758880.getLast().getTextBlocks();
        for (TextBlockVersion currentTextBlock : textBlocks) {
            if(currentTextBlock.getPred() != null){
                predecessorList.add(currentTextBlock.getPred().getLocalId());
            }
        }

        // if the list and the set do not have equal size, there exist duplicates (i.e., post blocks with multiple predecessors)
        Set<Integer> predecessorSet = new HashSet<>(predecessorList);
        assertTrue(predecessorList.size() == predecessorSet.size());
    }

    @Test
    void testPredecessorAssignmentQuestion37625877(){
        // tests predecessor assignment if two versions have two equal text blocks
        PostVersionList q_37625877 = new PostVersionList();
        q_37625877.readFromCSV("testdata", 37625877, 1, false);

        TextBlockVersion.similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceVariant;
        q_37625877.processVersionHistory(PostVersionList.PostBlockTypeFilter.TEXT);

        PostVersion version_1 = q_37625877.get(0);
        PostVersion version_2 = q_37625877.get(1);

        // both versions have two text blocks with value "or:" at position 2 and 3, which should be linked accordingly
        assertEquals(version_1.getTextBlocks().get(2).getLocalId(), version_2.getTextBlocks().get(2).getPred().getLocalId());
        assertEquals(version_1.getTextBlocks().get(3).getLocalId(), version_2.getTextBlocks().get(3).getPred().getLocalId());
    }

    @Test
    void testPredecessorAssignmentAnswer42070509(){
        // tests predecessor assignment if version i has three code blocks that are equal to four code blocks in version i+1
        PostVersionList a_42070509 = new PostVersionList();
        a_42070509.readFromCSV("testdata", 42070509, 2, false);

        a_42070509.processVersionHistory(PostVersionList.PostBlockTypeFilter.CODE);

        PostVersion version_1 = a_42070509.get(0);
        PostVersion version_2 = a_42070509.get(1);

        // code blocks with content "var circle = d3.select("circle");..." are present in both versions
        assertEquals(version_1.getCodeBlocks().get(0).getLocalId(), version_2.getCodeBlocks().get(0).getPred().getLocalId());
        assertEquals(version_1.getCodeBlocks().get(2).getLocalId(), version_2.getCodeBlocks().get(2).getPred().getLocalId());
        assertEquals(version_1.getCodeBlocks().get(4).getLocalId(), version_2.getCodeBlocks().get(4).getPred().getLocalId());
        // this code block is new in version two and should not have a predecessor
        assertEquals(null, version_2.getCodeBlocks().get(6).getPred());
    }


    @Test
    void testPredecessorAssignmentQuestion23459881(){
        PostVersionList q_23459881 = new PostVersionList();
        q_23459881.readFromCSV("testdata", 23459881, 1, true);

        PostVersion version_2 = q_23459881.get(1);
        assertEquals(8, version_2.getPostBlocks().size());
        assertEquals(4, version_2.getTextBlocks().size());
        assertEquals(4, version_2.getCodeBlocks().size());
        List<PostBlockVersion> postBlocks = version_2.getPostBlocks();
        assertTrue(postBlocks.get(0) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(1) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(2) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(3) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(4) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(5) instanceof CodeBlockVersion);
        assertTrue(postBlocks.get(6) instanceof TextBlockVersion);
        assertTrue(postBlocks.get(7) instanceof CodeBlockVersion);

        // version 1: text block with localId 3 and content "Code"
        // version 2: text block with localId 3 and content "Code" + same content in text block with localId 5
        // block 5 is "correct" successor
        assertNull(postBlocks.get(2).getPred()); // localId 3
        assertNull(postBlocks.get(3).getPred()); // localId 4

        assertNotNull(postBlocks.get(4).getPred()); // localId 5
        assertEquals(new Integer(3), postBlocks.get(4).getPred().getLocalId()); // localId 5

        assertNotNull(postBlocks.get(5).getPred()); // localId 6
        assertEquals(new Integer(4), postBlocks.get(5).getPred().getLocalId()); // localId 6

        assertNotNull(postBlocks.get(6).getPred()); // localId 7
        assertEquals(new Integer(5), postBlocks.get(6).getPred().getLocalId()); // localId 7

        assertNotNull(postBlocks.get(7).getPred()); // localId 8
        assertEquals(new Integer(6), postBlocks.get(7).getPred().getLocalId()); // localId 8
    }


    @Test
    void testPredecessorAssignmentQuestion36082771(){
        PostVersionList q_36082771 = new PostVersionList();
        q_36082771.readFromCSV("testdata", 36082771, 1, true);

        //TODO: revise

        PostVersion version_2 = q_36082771.get(1);

        assertEquals(new Integer(7), version_2.getPostBlocks().get(2).getPred().getLocalId());
        assertEquals(new Integer(8), version_2.getPostBlocks().get(3).getPred().getLocalId());
        assertEquals(new Integer(9), version_2.getPostBlocks().get(4).getPred().getLocalId());
        assertEquals(new Integer(10), version_2.getPostBlocks().get(5).getPred().getLocalId());
        assertEquals(new Integer(11), version_2.getPostBlocks().get(6).getPred().getLocalId());
        assertEquals(new Integer(12), version_2.getPostBlocks().get(7).getPred().getLocalId());
        assertEquals(new Integer(13), version_2.getPostBlocks().get(8).getPred().getLocalId());
        assertEquals(new Integer(14), version_2.getPostBlocks().get(9).getPred().getLocalId());
        assertEquals(new Integer(15), version_2.getPostBlocks().get(10).getPred().getLocalId());
        assertEquals(new Integer(16), version_2.getPostBlocks().get(11).getPred().getLocalId());
    }


    @Test
    void testPredecessorAssignmentQuestion18276636(){
        PostVersionList q_18276636 = new PostVersionList();
        q_18276636.readFromCSV("testdata", 18276636, 1, true);

        //TODO: revise

        PostVersion version_2 = q_18276636.get(1);

        assertEquals(new Integer(1), version_2.getPostBlocks().get(4).getPred().getLocalId());
        assertEquals(new Integer(2), version_2.getPostBlocks().get(5).getPred().getLocalId());
        assertEquals(new Integer(3), version_2.getPostBlocks().get(6).getPred().getLocalId());
        assertEquals(new Integer(4), version_2.getPostBlocks().get(7).getPred().getLocalId());
        assertEquals(new Integer(5), version_2.getPostBlocks().get(8).getPred().getLocalId());
        assertEquals(new Integer(6), version_2.getPostBlocks().get(9).getPred().getLocalId());
        assertEquals(new Integer(7), version_2.getPostBlocks().get(10).getPred().getLocalId());
        assertEquals(new Integer(8), version_2.getPostBlocks().get(11).getPred().getLocalId());
        assertEquals(new Integer(9), version_2.getPostBlocks().get(12).getPred().getLocalId());
        assertEquals(new Integer(10), version_2.getPostBlocks().get(13).getPred().getLocalId());
        assertEquals(new Integer(11), version_2.getPostBlocks().get(14).getPred().getLocalId());
        assertEquals(new Integer(12), version_2.getPostBlocks().get(15).getPred().getLocalId());
        assertEquals(new Integer(13), version_2.getPostBlocks().get(16).getPred().getLocalId());
    }
}
