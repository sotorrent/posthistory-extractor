package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostVersionHistoryTest {

    private static PostVersionList p_1109108;
    private static PostVersionList p_3145655;
    private static PostVersionList p_9855338;
    private static PostVersionList p_2581754;
    private static PostVersionList p_20991163;

    @BeforeAll
    static void init() {
        p_1109108 = new PostVersionList();
        p_1109108.readFromCSV("testdata/", 1109108, 2);
        p_3145655 = new PostVersionList();
        p_3145655.readFromCSV("testdata/", 3145655, 2);
        p_9855338 = new PostVersionList();
        p_9855338.readFromCSV("testdata/", 9855338, 2);
        p_2581754 = new PostVersionList();
        p_2581754.readFromCSV("testdata/", 2581754, 2);
        p_20991163 = new PostVersionList();
        p_20991163.readFromCSV("testdata/", 20991163, 2);
    }

    @Test
    void testReadingPostHistory1109108() {
        assertEquals(7, p_1109108.size());

        PostVersion version_7 = p_1109108.get(6);
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
        assertEquals(7, p_3145655.size());

        PostVersion version_7 = p_3145655.get(6);
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
        assertEquals(11, p_9855338.size());

        PostVersion version_11 = p_9855338.get(10);
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
        assertEquals(8, p_2581754.size());

        PostVersion version_3 = p_2581754.get(2);
        assertEquals(6, version_3.getPostBlocks().size());
        assertEquals(3, version_3.getTextBlocks().size());
        assertEquals(3, version_3.getCodeBlocks().size());
        System.out.println("version 4 block 7: " + p_2581754.get(3).getPostBlocks().get(6).getContent());

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

        System.out.println("text block version 8, block 7: " + p_2581754.get(7).getPostBlocks().get(6).getContent());

        // System.out.println("text block version 7, block 8: " + p_2581754.get(6).getPostBlocks().get(7).getContent());
    }

    @Test
    void testPostBlockVersionExtraction() {
        // TODO: add test cases for testing the version history extraction (similarity and diffs between code blocks and text blocks, linking of block versions)

        //p_1109108.processVersionHistory();
        //PostVersion version_7 = p_1109108.get(6);
        //assertEquals(2, version_7.getCodeBlocks().get(0).getPredDiff().size());
        // ....
    }

    @Test
    void testReadingPostHistory20991163() {
        assertEquals(1, p_20991163.size());

        PostVersion version_1 = p_20991163.get(0);
        assertEquals(1, version_1.getPostBlocks().size());
        assertEquals(0, version_1.getTextBlocks().size());
        assertEquals(1, version_1.getCodeBlocks().size());

        System.out.println("version 1: " + version_1.getContent());
    }

}