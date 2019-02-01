import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockSimilarity;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.version.PostVersion;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUtils {
    static Path pathToPostVersionLists = Paths.get("testdata", "post_version_lists");

    static Config configEqual = Config.DEFAULT
            .withTextSimilarityMetric(org.sotorrent.stringsimilarity.equal.Variants::equal)
            .withTextSimilarityThreshold(1.0)
            .withTextBackupSimilarityMetric(null)
            .withTextBackupSimilarityThreshold(1.0)
            .withCodeSimilarityMetric(org.sotorrent.stringsimilarity.equal.Variants::equal)
            .withCodeSimilarityThreshold(1.0)
            .withCodeBackupSimilarityMetric(null)
            .withCodeBackupSimilarityThreshold(1.0);

    static void testPredecessorSimilarities(PostVersion postVersion) {
        for (PostBlockVersion currentPostBlockVersion : postVersion.getPostBlocks()) {
            for (PostBlockSimilarity similarity : currentPostBlockVersion.getPredecessorSimilarities().values()) {
                assertThat(similarity.getMetricResult(), either(
                        allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)))
                        .or(equalTo(PostBlockVersion.EQUALITY_SIMILARITY))
                );
            }
        }
    }

    static void testPostBlockCount(PostVersion postVersion,
                                    int expectedPostBlockCount, int expectedTextBlockCount, int expectedCodeBlockCount) {
        assertEquals(expectedPostBlockCount, postVersion.getPostBlocks().size());
        assertEquals(expectedTextBlockCount, postVersion.getTextBlocks().size());
        assertEquals(expectedCodeBlockCount, postVersion.getCodeBlocks().size());
    }

    static void testPostBlockCount(PostVersion postVersion, int expectedTextBlockCount, int expectedCodeBlockCount) {
        assertEquals(expectedTextBlockCount+expectedCodeBlockCount, postVersion.getPostBlocks().size());
        assertEquals(expectedTextBlockCount, postVersion.getTextBlocks().size());
        assertEquals(expectedCodeBlockCount, postVersion.getCodeBlocks().size());
    }

    /**
     * Test order of post block types using fixed array.
     * @param postVersion Post version to test.
     * @param expectedPostBlockTypes Array with expected post block types.
     */
    static void testPostBlockTypes(PostVersion postVersion, Class[] expectedPostBlockTypes) {
        List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();
        // there has to be an expected type for each post block in this version
        assertEquals(postBlocks.size(), expectedPostBlockTypes.length);
        // test order of post block types
        for (int i=0; i<postBlocks.size(); i++) {
            assertEquals(postBlocks.get(i).getClass(), expectedPostBlockTypes[i]);
        }
    }

    /**
     * Test order of post block types using alternating order.
     * @param postVersion Post version to test.
     * @param firstPostBlockType Type to start with.
     */
    static void testPostBlockTypes(PostVersion postVersion, Class firstPostBlockType) {
        List<PostBlockVersion> postBlocks = postVersion.getPostBlocks();
        Class secondPostBlockType = firstPostBlockType == TextBlockVersion.class ? CodeBlockVersion.class : TextBlockVersion.class;
        for (int i=0; i<postBlocks.size(); i++) {
            if (i % 2 == 0) {
                assertEquals(postBlocks.get(i).getClass(), firstPostBlockType);
            } else {
                assertEquals(postBlocks.get(i).getClass(), secondPostBlockType);
            }
        }
    }
}
