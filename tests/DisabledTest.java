import com.google.common.collect.Sets;
import org.sotorrent.posthistoryextractor.comments.CommentsIterator;
import org.sotorrent.posthistoryextractor.history.PostHistory;
import org.sotorrent.posthistoryextractor.history.PostHistoryIterator;
import org.sotorrent.posthistoryextractor.history.PostHistoryList;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;
import org.sotorrent.posthistoryextractor.version.TitleVersionList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.sotorrent.util.LogUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Disabled
class DisabledTest {
    private static Logger logger;
    private static final Path pathToHibernateConfig = Paths.get("hibernate", "hibernate.cfg.xml");
    private static final String SOTORRENT_VERSION = "sotorrent_2020-08-31";
    // these posts are ignored below because they have a separate test case
    private static final Set<Integer> POSTS_WITH_TEST_CASE = Sets.newHashSet(5864258, 9875710);
    // posts without post blocks
    private static final Path posts_no_blocks = Paths.get("testdata", SOTORRENT_VERSION, "all_posts_no_blocks.csv");
    // posts without content versions
    private static final Path posts_no_content_versions = Paths.get("testdata", SOTORRENT_VERSION, "all_posts_no_content_versions.csv");
    // questions without title versions
    private static final Path questions_no_title_versions = Paths.get("testdata", SOTORRENT_VERSION, "all_questions_no_title_versions.csv");

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(DisabledTest.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void setupHibernate() {
        if (PostHistoryIterator.sessionFactory == null) {
            PostHistoryIterator.createSessionFactory(pathToHibernateConfig);
        }

        if (CommentsIterator.sessionFactory == null) {
            CommentsIterator.createSessionFactory(pathToHibernateConfig);
        }
    }

    @Test
    void testPostVersionsWithoutContent() {
        // posts without post blocks
        testPostBlockExtraction(posts_no_blocks, PostHistoryIterator.csvFormatPost);
        // posts without post versions
        testPostBlockExtraction(posts_no_content_versions, PostHistoryIterator.csvFormatVersion);
        testPostBlockExtraction(questions_no_title_versions, PostHistoryIterator.csvFormatVersion);
    }

    private void testPostBlockExtraction(Path pathToCSV, CSVFormat csvFormat) {
        if (PostHistoryList.sessionFactory == null) {
            PostHistoryList.createSessionFactory(pathToHibernateConfig);
        }

        // read post ids of posts without post blocks or versions from CSV file
        File inputFile = pathToCSV.toFile();
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Error while reading input file: " + inputFile);
        }

        boolean emptyPostBlockListPresent = false;
        boolean emptyPostVersionListPresent = false;
        boolean emptyTitleVersionListPresent = false;

        try (StatelessSession session = PostHistoryList.sessionFactory.openStatelessSession()) {
            try (CSVParser csvParser = new CSVParser(
                    new FileReader(inputFile), csvFormat.withFirstRecordAsHeader())) {

                // read all records into memory
                List<CSVRecord> records = csvParser.getRecords();
                int recordCount = records.size();

                logger.info(recordCount + " posts read.");

                // iterate over records
                for (CSVRecord record : records) {
                    int postId = Integer.parseInt(record.get(0));
                    byte postTypeId = Byte.parseByte(record.get(1));

                    // for some posts, there is a separate test case
                    if (POSTS_WITH_TEST_CASE.contains(postId)) {
                        continue;
                    }

                    if (postTypeId == 1 || postTypeId == 2) { // question or answer
                        // retrieve data from post history...
                        PostVersionList postVersions = new PostVersionList(postId, postTypeId);
                        TitleVersionList titleVersions = null;
                        if (postTypeId == Posts.QUESTION_ID) {
                            titleVersions = new TitleVersionList(postId, postTypeId);
                        }

                        // get all PostHistory entries for current PostId, order them chronologically
                        String currentPostHistoryQuery = String.format("FROM PostHistory WHERE PostId=%d", postId);

                        ScrollableResults postHistoryIterator = session.createQuery(currentPostHistoryQuery)
                                .scroll(ScrollMode.FORWARD_ONLY);

                        int contentVersionCount = 0;
                        int titleVersionCount = 0;

                        while (postHistoryIterator.next()) {
                            PostHistory currentPostHistoryEntity = (PostHistory) postHistoryIterator.get(0);

                            if (PostHistory.contentPostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                contentVersionCount++;
                            } else if (PostHistory.titlePostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                titleVersionCount++;
                            }

                            String text = currentPostHistoryEntity.getText();
                            // ignore versions that don't have any content (or only whitespaces)
                            if (text == null
                                    || text.replaceAll(PostHistory.newLineRegex, "").trim().length() == 0) {
                                continue;
                            }

                            if (PostHistory.contentPostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                currentPostHistoryEntity.extractPostBlocks();
                                PostVersion currentPostVersion = currentPostHistoryEntity.toPostVersion(postTypeId);

                                if (currentPostVersion.getPostBlocks().size() == 0) {
                                    logger.warning("No post blocks extracted for PostId: " + postId + "; PostHistoryId: " + currentPostVersion.getPostHistoryId());
                                    emptyPostBlockListPresent = true;
                                }

                                currentPostVersion.extractUrlsFromTextBlocks();
                                postVersions.add(currentPostVersion);
                            } else if (PostHistory.titlePostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                if (titleVersions != null) {
                                    titleVersions.add(currentPostHistoryEntity.toTitleVersion(postTypeId));
                                }
                            }
                        }

                        // ignore test case if no information about content versions of post available in table PostHistory
                        if (contentVersionCount == 0) {
                            //logger.info("No content versions available in table PostHistory for PostId " + postId);
                            continue;
                        }

                        // ignore test case if no information about title versions of post available in table PostHistory
                        if (titleVersionCount == 0) {
                            //logger.info("No title versions available in table PostHistory for PostId " + postId);
                            continue;
                        }

                        if (postVersions.size() == 0) {
                            logger.warning("No content versions extracted for PostId " + postId);
                            emptyPostVersionListPresent = true;
                        } else {
                            postVersions.sort();
                            postVersions.processVersionHistory();
                        }

                        assertNotNull(titleVersions);
                        if (postTypeId == Posts.QUESTION_ID && titleVersions.size() == 0) {
                            logger.warning("No title versions extracted for PostId " + postId);
                            emptyTitleVersionListPresent = true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("All posts processed.");

        assertFalse(emptyPostBlockListPresent);
        assertFalse(emptyPostVersionListPresent);
        assertFalse(emptyTitleVersionListPresent);
    }

    @Test
    void testPostVersionCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String postVersionPostIdsQuery = "select count(*) from PostVersion";
            long postVersionPostIds = (long) session.createQuery(postVersionPostIdsQuery).list().get(0);

            String postVersionPostHistoryIdsQuery = "select count(distinct PostHistoryId) from PostVersion";
            long postVersionPostHistoryIds = (long)session.createQuery(postVersionPostHistoryIdsQuery).list().get(0);

            assertThat(postVersionPostIds, allOf(equalTo(77783645L), equalTo(postVersionPostHistoryIds)));
        }
    }

    @Test
    void testPostIdCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String postVersionDistinctPostIdsQuery = "select count(distinct PostId) from PostVersion";
            long postVersionDistinctPostIds = (long) session.createQuery(postVersionDistinctPostIdsQuery).list().get(0);

            String postBlockVersionPostIdsQuery = "select count(distinct PostId) from PostBlockVersion";
            long postBlockVersionPostIds = (long)session.createQuery(postBlockVersionPostIdsQuery).list().get(0);

            assertThat(postVersionDistinctPostIds, allOf(equalTo(49036211L), equalTo(postBlockVersionPostIds)));
        }
    }

    @Test
    void testPostBlockVersionCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String postBlockVersionsQuery = "select count(*) from PostBlockVersion";
            long postBlockVersions = (long) session.createQuery(postBlockVersionsQuery).list().get(0);
            assertEquals(243993989, postBlockVersions);
        }
    }

    @Test
    void testPostVersionUrlCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String postVersionUrlsQuery = "select count(*) from PostVersionUrl";
            long postVersionUrls = (long) session.createQuery(postVersionUrlsQuery).list().get(0);
            assertEquals(38402277, postVersionUrls);
        }
    }

    @Test
    void testTitleVersionCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String titleVersionQuery = "select count(*) from TitleVersion";
            long titleVersions = (long) session.createQuery(titleVersionQuery).list().get(0);
            assertEquals(22585364, titleVersions);
        }
    }

    @Test
    void testPostReferenceGHCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String postReferenceGHQuery = "select count(*) from PostReferenceGH";
            long postReferenceGH = (long) session.createQuery(postReferenceGHQuery).list().get(0);
            assertEquals(8008892, postReferenceGH);
        }
    }

    @Test
    void tesGHMatchesCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String ghMatchesQuery = "select count(*) from GHMatches";
            long ghMatches = (long) session.createQuery(ghMatchesQuery).list().get(0);
            assertEquals(690042, ghMatches);
        }
    }

    @Test
    void tesGHCommitsCount() {
        try (StatelessSession session = PostHistoryIterator.sessionFactory.openStatelessSession()) {
            String ghCommitsQuery = "select count(*) from GHCommits";
            long ghCommits = (long) session.createQuery(ghCommitsQuery).list().get(0);
            assertEquals(396552, ghCommits);
        }
    }

    @Test
    void testCommentCount() {
        try (StatelessSession session = CommentsIterator.sessionFactory.openStatelessSession()) {
            String commentUrlQuery = "select count(*) from CommentUrl";
            long commentUrls = (long) session.createQuery(commentUrlQuery).list().get(0);
            assertEquals(8376074, commentUrls);
        }
    }
}