import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PredecessorAssignmentTest {

    @Test
    void testPredecessorAssignmentAnswer3758880() {
        // tests if posts blocks are set more than once as predecessor
        PostVersionList a_3758880 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 3758880, Posts.ANSWER_ID, false);

        a_3758880.processVersionHistory(TextBlockVersion.getPostBlockTypeIdFilter());

        // we consider the most recent version here
        List<Integer> predecessorList = new LinkedList<>();
        List<TextBlockVersion> textBlocks = a_3758880.getLast().getTextBlocks();
        for (TextBlockVersion currentTextBlock : textBlocks) {
            if (currentTextBlock.getPred() != null) {
                predecessorList.add(currentTextBlock.getPred().getLocalId());
            }
        }

        // if the list and the set do not have equal size, there exist duplicates (i.e., post blocks with multiple predecessors)
        Set<Integer> predecessorSet = new HashSet<>(predecessorList);
        assertEquals(predecessorList.size(), predecessorSet.size());
    }

    @Test
    void testPredecessorAssignmentQuestion37625877() {
        // tests predecessor assignment if two versions have two equal text blocks
        PostVersionList q_37625877 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 37625877, Posts.QUESTION_ID, false);

        q_37625877.processVersionHistory(TextBlockVersion.getPostBlockTypeIdFilter());

        PostVersion version_1 = q_37625877.get(0);
        PostVersion version_2 = q_37625877.get(1);
        TestUtils.testPredecessorSimilarities(version_2);

        // both versions have two text blocks with value "or:" at position 2 and 3, which should be linked accordingly
        Assertions.assertEquals(version_1.getTextBlocks().get(2).getLocalId(), version_2.getTextBlocks().get(2).getPred().getLocalId());
        Assertions.assertEquals(version_1.getTextBlocks().get(3).getLocalId(), version_2.getTextBlocks().get(3).getPred().getLocalId());
    }

    @Test
    void testPredecessorAssignmentAnswer42070509() {
        // tests predecessor assignment if version i has three code blocks that are equal to four code blocks in version i+1
        PostVersionList a_42070509 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 42070509, Posts.ANSWER_ID, false);

        a_42070509.processVersionHistory(CodeBlockVersion.getPostBlockTypeIdFilter());

        PostVersion version_1 = a_42070509.get(0);
        PostVersion version_2 = a_42070509.get(1);
        TestUtils.testPredecessorSimilarities(version_2);

        // code blocks with content "var circle = d3.select("circle");..." are present in both versions
        Assertions.assertEquals(version_1.getCodeBlocks().get(0).getLocalId(), version_2.getCodeBlocks().get(0).getPred().getLocalId());
        Assertions.assertEquals(version_1.getCodeBlocks().get(2).getLocalId(), version_2.getCodeBlocks().get(2).getPred().getLocalId());
        Assertions.assertEquals(version_1.getCodeBlocks().get(4).getLocalId(), version_2.getCodeBlocks().get(4).getPred().getLocalId());
        // this code block is new in version two and should not have a predecessor
        assertNull(version_2.getCodeBlocks().get(6).getPred());
    }


    @Test
    void testPredecessorAssignmentQuestion23459881() {
        PostVersionList q_23459881 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 23459881, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_23459881.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 8, 4, 4);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks = version_2.getPostBlocks();

        // version 1: text block with localId 3 and content "Code"
        // version 2: text block with localId 3 and content "Code" + same content in text block with localId 5
        // block 5 is "correct" successor
        assertNull(postBlocks.get(2).getPred()); // localId 3
        assertNull(postBlocks.get(3).getPred()); // localId 4

        assertNotNull(postBlocks.get(4).getPred()); // localId 5
        assertEquals(Integer.valueOf(3), postBlocks.get(4).getPred().getLocalId()); // localId 5

        assertNotNull(postBlocks.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(4), postBlocks.get(5).getPred().getLocalId()); // localId 6

        assertNotNull(postBlocks.get(6).getPred()); // localId 7
        assertEquals(Integer.valueOf(5), postBlocks.get(6).getPred().getLocalId()); // localId 7

        assertNotNull(postBlocks.get(7).getPred()); // localId 8
        assertEquals(Integer.valueOf(6), postBlocks.get(7).getPred().getLocalId()); // localId 8
    }


    @Test
    void testPredecessorAssignmentQuestion36082771() {
        PostVersionList q_36082771 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 36082771, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_36082771.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 12, 6, 6);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks = version_2.getPostBlocks();

        // version 1: code block with localId 2 and content "glaucon@polo..." + same content in code block with localId 6
        // version 2: code block with localId 2 and content "glaucon@polo..."
        // block 2 is "correct" predecessor
        assertNotNull(postBlocks.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks.get(1).getPred().getLocalId()); // localId 2

        // version 1: text block with localId 3 and content "If I do then run..." + same content in code block with localId 7
        // version 2: code block with localId 3 and content "If I do then run..."
        // block 3 is "correct" predecessor
        assertNotNull(postBlocks.get(2).getPred()); // localId 3
        assertEquals(Integer.valueOf(3), postBlocks.get(2).getPred().getLocalId()); // localId 3

        // version 1: text block with localId 4 and content "glaucon@polo..." + same content in code block with localId 8
        // version 2: code block with localId 4 and content "glaucon@polo..."
        // block 4 is "correct" predecessor
        assertNotNull(postBlocks.get(3).getPred()); // localId 4
        assertEquals(Integer.valueOf(4), postBlocks.get(3).getPred().getLocalId()); // localId 4
    }


    @Test
    void testPredecessorAssignmentQuestion18276636() {
        PostVersionList q_18276636 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 18276636, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_18276636.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 17, 9, 8);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks = version_2.getPostBlocks();
        // version 1: text block with localId 3 and content "to"
        // version 2: text block with localId 3 and content "to" + same content in text block with localId 7
        // block 7 is "correct" successor (same neighbors)
        assertNull(postBlocks.get(2).getPred()); // localId 3
        assertNotNull(postBlocks.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(2), postBlocks.get(5).getPred().getLocalId()); // localId 6
        assertNotNull(postBlocks.get(6).getPred()); // localId 7
        assertEquals(Integer.valueOf(3), postBlocks.get(6).getPred().getLocalId()); // localId 7
        assertNotNull(postBlocks.get(7).getPred()); // localId 8
        assertEquals(Integer.valueOf(4), postBlocks.get(7).getPred().getLocalId()); // localId 8
    }

    @Test
    void testPredecessorAssignmentAnswer1870600() {
        PostVersionList q_1870600 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 1870600, Posts.QUESTION_ID, true);

        PostVersion version_5 = q_1870600.get(4);
        TestUtils.testPredecessorSimilarities(version_5);
        TestUtils.testPostBlockCount(version_5, 9, 5, 4);
        TestUtils.testPostBlockTypes(version_5, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_5 = version_5.getPostBlocks();
        assertNotNull(postBlocks_version_5.get(3).getPred()); // localId 4
        assertEquals(Integer.valueOf(4), postBlocks_version_5.get(3).getPred().getLocalId()); // localId 4
        assertNull(postBlocks_version_5.get(5).getPred()); // localId 6

        PostVersion version_6 = q_1870600.get(5);
        TestUtils.testPostBlockTypes(version_5, TextBlockVersion.class);
        List<PostBlockVersion> postBlocks_version_6 = version_6.getPostBlocks();
        assertNotNull(postBlocks_version_6.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(6), postBlocks_version_6.get(5).getPred().getLocalId()); // localId 6
        assertNull(postBlocks_version_6.get(7).getPred()); // localId 8
    }

    @Test
    void testPredecessorAssignmentAnswer8432848() {
        PostVersionList q_8432848 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 8432848, Posts.QUESTION_ID, true);

        PostVersion version_4 = q_8432848.get(3);
        TestUtils.testPredecessorSimilarities(version_4);
        TestUtils.testPostBlockCount(version_4, 5, 3, 2);
        TestUtils.testPostBlockTypes(version_4, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_4 = version_4.getPostBlocks();
        assertNotNull(postBlocks_version_4.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_4.get(1).getPred().getLocalId()); // localId 2
        assertNull(postBlocks_version_4.get(3).getPred()); // localId 4
    }

    @Test
    void testPostBlockExtractionAnswer17158055() {
        PostVersionList q_17158055 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 17158055, Posts.QUESTION_ID, true);

        PostVersion version_6 = q_17158055.get(5);
        TestUtils.testPredecessorSimilarities(version_6);
        TestUtils.testPostBlockCount(version_6, 17, 9, 8);
        TestUtils.testPostBlockTypes(version_6, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_6 = version_6.getPostBlocks();
        assertEquals("`mydomain.com/bn/products/1`", postBlocks_version_6.get(3).getContent().trim());
    }

    @Test
    void testPredecessorAssignmentAnswer17158055() {
        PostVersionList q_17158055 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 17158055, Posts.QUESTION_ID, true);

        PostVersion version_7 = q_17158055.get(6);
        TestUtils.testPredecessorSimilarities(version_7);
        TestUtils.testPostBlockCount(version_7, 17, 9, 8);
        TestUtils.testPostBlockTypes(version_7, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_7 = version_7.getPostBlocks();
        assertNotNull(postBlocks_version_7.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_7.get(1).getPred().getLocalId()); // localId 2
        assertNotNull(postBlocks_version_7.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(6), postBlocks_version_7.get(5).getPred().getLocalId()); // localId 6
    }

    @Test
    void testPredecessorAssignmentAnswer25488162() {
        PostVersionList q_25488162 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 25488162, Posts.QUESTION_ID, true);

        PostVersion version_3 = q_25488162.get(2);
        TestUtils.testPredecessorSimilarities(version_3);
        TestUtils.testPostBlockCount(version_3, 6, 3, 3);
        TestUtils.testPostBlockTypes(version_3, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_3 = version_3.getPostBlocks();
        assertNotNull(postBlocks_version_3.get(2).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_3.get(1).getPred().getLocalId()); // localId 2
        // the current default metric cannot detect this (similarity is 0.2)
        //assertNotNull(postBlocks_version_3.get(3).getPred()); // localId 4
        //assertEquals(Integer.valueOf(4), postBlocks_version_3.get(3).getPred().getLocalId()); // localId 4
    }

    @Test
    void testPredecessorAssignmentAnswer39313130() {
        PostVersionList q_39313130 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 39313130, Posts.QUESTION_ID, true);

        PostVersion version_7 = q_39313130.get(6);
        TestUtils.testPredecessorSimilarities(version_7);
        TestUtils.testPostBlockCount(version_7, 2, 1, 1);
        TestUtils.testPostBlockTypes(version_7, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_7 = version_7.getPostBlocks();
        assertNotNull(postBlocks_version_7.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_7.get(1).getPred().getLocalId()); // localId 2
    }

    @Test
    void testPredecessorAssignmentQuestion33137697() {
        PostVersionList q_33137697 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 33137697, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_33137697.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 12, 6, 6);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_2 = version_2.getPostBlocks();
        assertNotNull(postBlocks_version_2.get(7).getPred()); // localId 8
        assertEquals(Integer.valueOf(8), postBlocks_version_2.get(7).getPred().getLocalId()); // localId 8
        assertNull(postBlocks_version_2.get(9).getPred()); // localId 10
    }

    @Test
    void testPredecessorAssignmentAnswer33003217() {
        PostVersionList q_33003217 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 33003217, Posts.QUESTION_ID, true);

        PostVersion version_8 = q_33003217.get(7);
        TestUtils.testPredecessorSimilarities(version_8);
        TestUtils.testPostBlockCount(version_8, 6, 3, 3);
        TestUtils.testPostBlockTypes(version_8, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_8 = version_8.getPostBlocks();
        assertNotNull(postBlocks_version_8.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_8.get(1).getPred().getLocalId()); // localId 2
        assertNull(postBlocks_version_8.get(3).getPred()); // localId 4
    }

    @Test
    void testPredecessorAssignmentAnswer32801275() {
        PostVersionList q_32801275 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 32801275, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_32801275.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 7, 4, 3);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_2 = version_2.getPostBlocks();
        assertNotNull(postBlocks_version_2.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_2.get(1).getPred().getLocalId()); // localId 2
        assertNull(postBlocks_version_2.get(3).getPred()); // localId 4

        PostVersion version_9 = q_32801275.get(8);
        TestUtils.testPredecessorSimilarities(version_9);
        TestUtils.testPostBlockCount(version_9, 7, 3, 4);
        TestUtils.testPostBlockTypes(version_9, new Class[]{
                TextBlockVersion.class,
                CodeBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class,
                CodeBlockVersion.class,
                CodeBlockVersion.class,
                TextBlockVersion.class
        });

        // TODO: Update GT according to this mapping
        List<PostBlockVersion> postBlocks_version_9 = version_9.getPostBlocks();
        assertNotNull(postBlocks_version_9.get(0).getPred()); // localId 1
        assertEquals(Integer.valueOf(1), postBlocks_version_9.get(0).getPred().getLocalId()); // localId 1
        assertNull(postBlocks_version_9.get(1).getPred()); // localId 2
        assertNull(postBlocks_version_9.get(2).getPred()); // localId 3
        assertNotNull(postBlocks_version_9.get(3).getPred()); // localId 4
        assertEquals(Integer.valueOf(3), postBlocks_version_9.get(3).getPred().getLocalId()); // localId 4
        assertNull(postBlocks_version_9.get(4).getPred()); // localId 5
        assertNotNull(postBlocks_version_9.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(6), postBlocks_version_9.get(5).getPred().getLocalId()); // localId 6
        assertNotNull(postBlocks_version_9.get(6).getPred()); // localId 7
        assertEquals(Integer.valueOf(7), postBlocks_version_9.get(6).getPred().getLocalId()); // localId 7
    }

    @Test
    void testPredecessorAssignmentAnswer29113416() {
        PostVersionList q_29113416 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 29113416, Posts.QUESTION_ID, true);

        PostVersion version_3 = q_29113416.get(2);
        TestUtils.testPredecessorSimilarities(version_3);
        TestUtils.testPostBlockCount(version_3, 11, 6, 5);
        TestUtils.testPostBlockTypes(version_3, TextBlockVersion.class);
        List<PostBlockVersion> postBlocks_version_3 = version_3.getPostBlocks();
        assertNotNull(postBlocks_version_3.get(9).getPred()); // localId 10
        assertEquals(Integer.valueOf(10), postBlocks_version_3.get(9).getPred().getLocalId()); // localId 10
    }

    @Test
    void testPredecessorAssignmentAnswer28623462() {
        PostVersionList q_28623462 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 28623462, Posts.QUESTION_ID, true);

        PostVersion version_6 = q_28623462.get(5);
        TestUtils.testPredecessorSimilarities(version_6);
        TestUtils.testPostBlockCount(version_6, 6, 3, 3);
        TestUtils.testPostBlockTypes(version_6, TextBlockVersion.class);
        List<PostBlockVersion> postBlocks_version_6 = version_6.getPostBlocks();
        assertNotNull(postBlocks_version_6.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_6.get(1).getPred().getLocalId()); // localId 2
        assertNull(postBlocks_version_6.get(3).getPred());
    }

    @Test
    @Disabled
        // see comment below
    void testPredecessorAssignmentAnswer26050416() {
        PostVersionList q_26050416 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 26050416, Posts.QUESTION_ID, true);

        PostVersion version_7 = q_26050416.get(6);
        TestUtils.testPredecessorSimilarities(version_7);
        TestUtils.testPostBlockCount(version_7, 7, 4, 3);
        TestUtils.testPostBlockTypes(version_7, TextBlockVersion.class);

        // this cannot be solved with the current default metric
        // (localId 4 in version 7 has similarity 0.89 with localId 6 in version 6, but only 0.86 with localId 4)
        List<PostBlockVersion> postBlocks_version_7 = version_7.getPostBlocks();
        assertNotNull(postBlocks_version_7.get(3).getPred()); // localId 4
        assertEquals(Integer.valueOf(4), postBlocks_version_7.get(3).getPred().getLocalId()); // localId 4
        assertNotNull(postBlocks_version_7.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(6), postBlocks_version_7.get(5).getPred().getLocalId()); // localId 6
    }

    @Test
    void testPredecessorAssignmentAnswer25871278() {
        PostVersionList q_25871278 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 25871278, Posts.QUESTION_ID, true);

        PostVersion version_6 = q_25871278.get(5);
        TestUtils.testPredecessorSimilarities(version_6);
        TestUtils.testPostBlockCount(version_6, 9, 5, 4);
        TestUtils.testPostBlockTypes(version_6, TextBlockVersion.class);
        List<PostBlockVersion> postBlocks_version_6 = version_6.getPostBlocks();
        assertNotNull(postBlocks_version_6.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(2), postBlocks_version_6.get(1).getPred().getLocalId()); // localId 2
        assertNull(postBlocks_version_6.get(3).getPred());
    }

    @Test
    void testPredecessorAssignmentAnswer15119106() {
        PostVersionList q_15119106 = PostVersionList.readFromCSV(TestUtils.pathToPostVersionLists, 15119106, Posts.QUESTION_ID, true);

        PostVersion version_2 = q_15119106.get(1);
        TestUtils.testPredecessorSimilarities(version_2);
        TestUtils.testPostBlockCount(version_2, 6, 3, 3);
        TestUtils.testPostBlockTypes(version_2, TextBlockVersion.class);

        List<PostBlockVersion> postBlocks_version_2 = version_2.getPostBlocks();
        assertNull(postBlocks_version_2.get(0).getPred()); // localId 1
        assertNotNull(postBlocks_version_2.get(1).getPred()); // localId 2
        assertEquals(Integer.valueOf(1), postBlocks_version_2.get(1).getPred().getLocalId()); // localId 2
        assertNotNull(postBlocks_version_2.get(2).getPred()); // localId 3
        assertEquals(Integer.valueOf(2), postBlocks_version_2.get(2).getPred().getLocalId()); // localId 3
        assertNull(postBlocks_version_2.get(3).getPred()); // localId 4
        assertNull(postBlocks_version_2.get(4).getPred()); // localId 5
        assertNotNull(postBlocks_version_2.get(5).getPred()); // localId 6
        assertEquals(Integer.valueOf(3), postBlocks_version_2.get(5).getPred().getLocalId()); // localId 6
    }
}