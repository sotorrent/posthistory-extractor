package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.history.PostHistory;
import de.unitrier.st.soposthistory.history.PostHistoryIterator;
import de.unitrier.st.soposthistory.history.PostHistoryList;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DisabledTests {
    private static Logger logger;
    private static Path sotorrent_2018_01_12 = Paths.get("testdata", "sotorrent_2018-01-12", "post_versions_no_blocks.csv");
    private static Path sotorrent_2018_01_12_questions = Paths.get("testdata", "sotorrent_2018-01-18", "all_questions_no_blocks.csv");
    private static Path pathToHibernateConfig = Paths.get("hibernate", "hibernate.cfg.xml");

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(DisabledTests.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Disabled
    @Test
    void testPostVersionsWithoutBlocks() {
        testPostBlockExtraction(sotorrent_2018_01_12, PostHistoryIterator.csvFormatPost);
        testPostBlockExtraction(sotorrent_2018_01_12_questions, PostHistoryIterator.csvFormatVersion);
    }

    private void testPostBlockExtraction(Path pathToCSV, CSVFormat csvFormat) {
        if (PostHistoryList.sessionFactory == null) {
            PostHistoryList.createSessionFactory(pathToHibernateConfig);
        }

        // read post ids of posts without post blocks from CSV file
        File inputFile = pathToCSV.toFile();
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Error while reading input file: " + inputFile);
        }

        boolean emptyPostBlockListPresent = false;
        boolean emptyPostVersionListPresent = false;

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
                    int postTypeId = Integer.parseInt(record.get(1));

                    if (postTypeId == 1 || postTypeId == 2) { // question or answer
                        // retrieve data from post history...
                        PostVersionList postVersionList = new PostVersionList(postId, postTypeId);

                        // get all PostHistory entries for current PostId, order them chronologically
                        String currentPostHistoryQuery = String.format("FROM PostHistory WHERE PostId=%d", postId);

                        ScrollableResults postHistoryIterator = session.createQuery(currentPostHistoryQuery)
                                .scroll(ScrollMode.FORWARD_ONLY);

                        while (postHistoryIterator.next()) {
                            PostHistory currentPostHistoryEntity = (PostHistory) postHistoryIterator.get(0);

                            // ignore versions that don't have any content
                            if (currentPostHistoryEntity.getText() == null || currentPostHistoryEntity.getText().length() == 0) {
                                continue;
                            }

                            currentPostHistoryEntity.extractPostBlocks();
                            currentPostHistoryEntity.setPostTypeId(postTypeId);
                            PostVersion currentPostVersion = currentPostHistoryEntity.toPostVersion();

                            if (currentPostVersion.getPostBlocks().size() == 0) {
                                logger.warning("No post blocks extracted for PostId: " + postId + "; PostHistoryId: " + currentPostVersion.getPostHistoryId());
                                emptyPostBlockListPresent = true;
                            }

                            currentPostVersion.extractUrlsFromTextBlocks();
                            postVersionList.add(currentPostVersion);
                        }

                        if (postVersionList.size() == 0) {
                            logger.warning("No versions extracted for PostId " + postId);
                            emptyPostVersionListPresent = true;
                        } else {
                            postVersionList.sort();
                            postVersionList.processVersionHistory();
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
    }
}