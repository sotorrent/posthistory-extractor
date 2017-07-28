package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostVersionHistoryTest {

    private static PostVersionList a_1109108;
    private static PostVersionList a_3145655;
    private static PostVersionList a_9855338;
    private static PostVersionList a_2581754;
    private static PostVersionList a_20991163;
    private static PostVersionList a_3758880;
    private static PostVersionList a_32012927;

    @BeforeAll
    static void init() {
        a_1109108 = new PostVersionList();
        a_1109108.readFromCSV("testdata/", 1109108, 2);
        a_3145655 = new PostVersionList();
        a_3145655.readFromCSV("testdata/", 3145655, 2);
        a_9855338 = new PostVersionList();
        a_9855338.readFromCSV("testdata/", 9855338, 2);
        a_2581754 = new PostVersionList();
        a_2581754.readFromCSV("testdata/", 2581754, 2);
        a_20991163 = new PostVersionList();
        a_20991163.readFromCSV("testdata/", 20991163, 2);
        a_3758880 = new PostVersionList();
        a_3758880.readFromCSV("testdata/", 3758880, 2);
        a_32012927 = new PostVersionList();
        a_32012927.readFromCSV("testdata/", 32012927, 2);
    }

    @Test
    void testReadingPostHistory1109108() {
        assertEquals(7, a_1109108.size());

        PostVersion version_7 = a_1109108.get(6);
        assertEquals(3, version_7.getPostBlocks().size());
        assertEquals(2, version_7.getTextBlocks().size());
        assertEquals(1, version_7.getCodeBlocks().size());
        //System.out.print(version_7.toString());

        CodeBlockVersion codeBlock_1 = version_7.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(6, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }
        //System.out.print(codeBlock_1.toString());

        TextBlockVersion textBlock_1 = version_7.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(1, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
        //System.out.print(textBlock_1.toString());
    }

    @Test
    void testReadingPostHistory3145655() {
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
    void testReadingPostHistory9855338() {
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
    void testReadingPostHistory2581754() {
        assertEquals(8, a_2581754.size());

        PostVersion version_3 = a_2581754.get(2);
        assertEquals(6, version_3.getPostBlocks().size());
        assertEquals(3, version_3.getTextBlocks().size());
        assertEquals(3, version_3.getCodeBlocks().size());
        System.out.println("version 4 block 7: " + a_2581754.get(3).getPostBlocks().get(6).getContent());

        CodeBlockVersion codeBlock_1 = version_3.getCodeBlocks().get(0);
        String[] lines = codeBlock_1.getContent().split("\n");
        assertEquals(25, lines.length);
        for (String line : lines) {
            assertTrue(line.startsWith("    "));
        }
        System.out.println("code block size: " + version_3.getCodeBlocks().size());

        TextBlockVersion textBlock_1 = version_3.getTextBlocks().get(0);
        lines = textBlock_1.getContent().split("\n");
        assertEquals(1, lines.length);
        for (String line : lines) {
            assertFalse(line.startsWith("    "));
        }
        System.out.println("text block size: " + version_3.getTextBlocks().size());

        System.out.println("text block version 8, block 7: " + a_2581754.get(7).getPostBlocks().get(6).getContent());

        // System.out.println("text block version 7, block 8: " + a_2581754.get(6).getPostBlocks().get(7).getContent());
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
    void testReadingPostHistory20991163() {
        // this post should only consist of one code block (not an empty text block at the end)

        assertEquals(1, a_20991163.size());

        PostVersion version_1 = a_20991163.get(0);
        assertEquals(1, version_1.getPostBlocks().size());
        assertEquals(0, version_1.getTextBlocks().size());
        assertEquals(1, version_1.getCodeBlocks().size());
    }

    @Test
    void testReadingPostHistory32012927() {
        assertEquals(4, a_32012927.size());

        // the first version of this post should only consist of one text block
        PostVersion version_1 = a_32012927.get(0);
        assertEquals(1, version_1.getPostBlocks().size());
        assertEquals(1, version_1.getTextBlocks().size());
        assertEquals(0, version_1.getCodeBlocks().size());
    }

    @Test
    void testRootPostBlockVersionId3758880() {
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
}