package org.sotorrent.posthistoryextractor.history;

import com.google.common.collect.Sets;
import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.diffs.PostBlockDiff;
import org.sotorrent.posthistoryextractor.urls.PostReferenceGH;
import org.sotorrent.posthistoryextractor.urls.PostVersionUrl;
import org.apache.commons.csv.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.sotorrent.posthistoryextractor.version.*;
import org.sotorrent.util.HibernateUtils;
import org.sotorrent.util.LogUtils;
import org.sotorrent.util.collections.CollectionUtils;
import org.sotorrent.util.exceptions.ErrorUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostHistoryIterator {

    private static Logger logger = null;
    public static final CSVFormat csvFormatPost;
    public static final CSVFormat csvFormatVersion;
    private static final int LOG_PACE = 1000;
    public static SessionFactory sessionFactory = null;

    private File dataDir;
    private String baseFilename;
    private int partitionCount;
    private String[] tags;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(PostHistoryIterator.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for in- and output
        csvFormatPost = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId")
                .withDelimiter(',')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("");

        // configure CSV format for output of versions without blocks
        csvFormatVersion = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId", "PostHistoryId")
                .withDelimiter(',')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("");
    }

    public PostHistoryIterator(Path dataDirPath, String baseFilename,
                               int partitionCount, String[] tags) {
        this.baseFilename = baseFilename;
        this.partitionCount = partitionCount;
        this.tags = tags;
        this.dataDir = dataDirPath.toFile();

        // ensure that data dir exists
        try {
            if (!Files.exists(dataDirPath)) {
                Files.createDirectory(dataDirPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createSessionFactory(Path hibernateConfigFilePath) {
        if (!Files.exists(hibernateConfigFilePath) || Files.isDirectory(hibernateConfigFilePath)) {
            throw new IllegalArgumentException("Not a valid Hibernate config file: " + hibernateConfigFilePath);
        }

        sessionFactory = new Configuration()
                .addAnnotatedClass(PostHistory.class)
                .addAnnotatedClass(PostVersion.class)
                .addAnnotatedClass(PostBlockVersion.class)
                .addAnnotatedClass(TextBlockVersion.class)
                .addAnnotatedClass(CodeBlockVersion.class)
                .addAnnotatedClass(PostBlockDiff.class)
                .addAnnotatedClass(Posts.class)
                .addAnnotatedClass(PostVersionUrl.class)
                .addAnnotatedClass(PostReferenceGH.class)
                .addAnnotatedClass(TitleVersion.class)
                .configure(hibernateConfigFilePath.toFile())
                .buildSessionFactory();
    }

    public void extractSaveAndSplitPostIds() {
        if (sessionFactory == null) {
            throw new IllegalStateException("Static session factory not created yet.");
        }

        // construct pattern that matches the provided tags
        String tagPattern = Arrays.stream(tags)
                .map(tag -> "LOWER(tags) LIKE '%<" + tag + ">%'")
                .collect(Collectors.joining(" OR "));

        Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
        try (StatelessSession session = sessionFactory.openStatelessSession()) {

            // retrieve question post ids
            String questionsPostIdQueryString;
            if (tagPattern.length() == 0) {
                // do not filter questions
                logger.info("Retrieving all questions from table Posts...");
                questionsPostIdQueryString = "SELECT id FROM Posts " +
                        "WHERE postTypeId = 1 ORDER BY id ASC";
            } else {
                // filter questions according to tags
                logger.info("Retrieving questions having configured tags from table Posts...");
                questionsPostIdQueryString = String.format("SELECT id FROM Posts " +
                        "WHERE postTypeId = 1 AND (%s) ORDER BY id ASC", tagPattern);
            }

            t = session.beginTransaction();
            Query questionsPostIdQuery = session.createQuery(questionsPostIdQueryString);
            @SuppressWarnings("unchecked") // see https://stackoverflow.com/a/509115
                    List<Integer> questionPostIds = questionsPostIdQuery.list();
            logger.info(questionPostIds.size() + " questions retrieved.");
            t.commit();
            // write question post ids to CSV file
            writePostIdsToCSV(questionPostIds, 1, baseFilename+"_questions.csv"); // questions have PostTypeId 1

            // retrieve answer post ids
            String answerPostIdQueryString;
            if (tagPattern.length() == 0) {
                // do not filter answers
                logger.info("Retrieving all answers from table Posts...");
                answerPostIdQueryString = "SELECT id FROM Posts " +
                        "WHERE postTypeId = 2 ORDER BY id ASC";
            } else {
                // filter answers according to tags
                logger.info("Retrieving answers to the previously retrieved questions from table Posts...");
                answerPostIdQueryString = String.format("SELECT id FROM Posts " +
                        "WHERE postTypeId = 2 AND parentId IN (%s) " +
                        "ORDER BY id ASC", questionsPostIdQueryString);
            }

            t = session.beginTransaction();
            Query answerPostIdQuery = session.createQuery(answerPostIdQueryString);
            @SuppressWarnings("unchecked") // see https://stackoverflow.com/a/509115
                    List<Integer> answerPostIds = answerPostIdQuery.list();
            logger.info(answerPostIds.size() + " answers retrieved.");
            t.commit();
            // write answer post ids to CSV file
            writePostIdsToCSV(answerPostIds, 2, baseFilename+"_answers.csv"); // answers have PostTypeId 2

        } catch (RuntimeException e) {
            if (t != null) {
                t.rollback();
                e.printStackTrace();
            }
        }

        // split up retrieved PostIds into partitions
        splitPostIds();
    }

    private void writePostIdsToCSV(List<Integer> postIds, int postTypeId, String filename) {
        File outputFile = Paths.get(dataDir.toString(), filename).toFile();
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFile);
            }
        }
        logger.info("Writing data to CSV file " + outputFile.getName() + " ...");
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), csvFormatPost)) {
            // header is automatically written
            // write postIds along with postTypeId
            for (int postId : postIds) {
                csvPrinter.printRecord(postId, postTypeId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> readPostIdsFromCSV(String filename) {
        List<Integer> postIds = null;

        File inputFile = Paths.get(dataDir.toString(), filename).toFile();
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Error while reading input file: " + inputFile);
        }
        logger.info("Reading file " + inputFile.getName() + " ...");

        try (CSVParser csvParser = new CSVParser(new FileReader(inputFile), csvFormatPost.withFirstRecordAsHeader())) {
            // read all records into memory
            List<CSVRecord> records = csvParser.getRecords();
            postIds = records.stream()
                    .map(r -> Integer.parseInt(r.get("PostId")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return postIds;
    }

    private void splitPostIds() {
        List<Integer> postIds;
        List<Integer>[] subLists;

        // split questions
        logger.info("Splitting questions...");
        postIds = readPostIdsFromCSV(baseFilename + "_questions.csv");
        subLists = CollectionUtils.split(postIds, partitionCount);
        for (int i=0; i<subLists.length; i++) {
            List<Integer> list = subLists[i];
            writePostIdsToCSV(list, 1, baseFilename + "_questions_" + i + ".csv");
        }
        logger.info("Splitting of questions complete.");

        // split answers
        logger.info("Splitting answers...");
        postIds = readPostIdsFromCSV(baseFilename + "_answers.csv");
        subLists = CollectionUtils.split(postIds, partitionCount);
        for (int i=0; i<subLists.length; i++) {
            List<Integer> list = subLists[i];
            writePostIdsToCSV(list, 2, baseFilename + "_answers_" + i + ".csv");
        }
        logger.info("Splitting of answers complete.");
    }

    public void extractDataFromPostHistory(String type) {
        List<ExtractionThread> extractionThreads = new LinkedList<>();

        logger.info("Starting parallel extraction of post history...");

        for (int i=0; i<partitionCount; i++) {
            ExtractionThread thread = new ExtractionThread(baseFilename + "_" + type, i);
            extractionThreads.add(thread);
            thread.start();
            logger.info("Thread " + i + " started...");
        }

        try {
            for (int i=0; i<extractionThreads.size(); i++) {
                extractionThreads.get(i).join();
                logger.info("Thread " + i + " terminated.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.info("Parallel extraction of post history finished.");
    }

    private class ExtractionThread extends Thread {
        private final String baseFilename;
        private final String filename;
        private final int partition;

        ExtractionThread(String baseFilename, int partition) {
            this.baseFilename = baseFilename;
            this.filename = baseFilename + "_" + partition + ".csv";
            this.partition = partition;
        }

        @Override
        public void run() {
            extractDataFromPostHistory(filename);
            logger.info("File " + filename + " has been processed.");
        }

        private void extractDataFromPostHistory(String filename) {
            if (sessionFactory == null) {
                throw new IllegalStateException("Static session factory not created yet.");
            }

            // read post ids from CSV file and process them
            File inputFile = Paths.get(dataDir.toString(), filename).toFile();
            if (!inputFile.exists()) {
                throw new IllegalArgumentException("Thread " + partition + ": Error while reading input file: "
                        + inputFile);
            }
            logger.info("Thread " + partition + ": Reading file " + inputFile.getName() + " ...");
            logger.info("Thread " + partition + ": Parsing history for provided PostIds and writing versions " +
                    "and diffs back to database...");

            // data structures to capture erroneous post histories

            // store posts without extracted versions
            List<VersionList> postsWithoutContentVersions = new LinkedList<>();
            int postsWithContentVersionsCount = 0;
            // stores post posts of questions without title versions
            List<VersionList> postsWithoutTitleVersions = new LinkedList<>();
            // store post versions without extracted post blocks
            List<PostVersion> postsVersionsWithoutBlocks = new LinkedList<>();

            // read all PostIds from the CSV file and extract history from table PostHistory
            Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
            try (StatelessSession session = sessionFactory.openStatelessSession()) {
                try (CSVParser csvParser = new CSVParser(
                        new FileReader(inputFile), csvFormatPost.withFirstRecordAsHeader())) {

                    // read all records into memory
                    List<CSVRecord> records = csvParser.getRecords();
                    int recordCount = records.size();

                    logger.info("Thread " + partition + ": " + recordCount + " posts read.");

                    // iterate over records
                    for (int i=0; i<recordCount; i++) {
                        CSVRecord record = records.get(i);
                        int postId = Integer.parseInt(record.get("PostId"));
                        byte postTypeId = Byte.parseByte(record.get("PostTypeId"));

                        // log only every LOG_PACE record
                        if (i == 0 || i == recordCount-1 || i % LOG_PACE == 0) {
                            // Locale.ROOT -> force '.' as decimal separator
                            String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(i+1))/recordCount*100));
                            logger.info( "Thread " + partition + ": Current PostId: " + postId
                                    + " (PostTypeId: " + postTypeId
                                    + "; record " + (i+1) + " of " + recordCount + "; " + progress + ")");
                        }

                        if (postTypeId == 1 || postTypeId == 2) { // question or answer
                            // retrieve data from post history about title and body changes
                            PostVersionList postVersionList = new PostVersionList(postId, postTypeId);
                            TitleVersionList titleVersionList = null;
                            if (postTypeId == Posts.QUESTION_ID) {
                                titleVersionList = new TitleVersionList(postId, postTypeId);
                            }

                            // get all PostHistory entries for current PostId
                            String currentPostHistoryQuery = String.format("FROM PostHistory " +
                                            "WHERE PostId=%d AND postHistoryTypeId IN (%s)",
                                    postId,
                                    HibernateUtils.setToQueryString(Sets.union(PostHistory.contentPostHistoryTypes,
                                            PostHistory.titlePostHistoryTypes))
                            );

                            t = session.beginTransaction();

                            ScrollableResults postHistoryIterator = session.createQuery(currentPostHistoryQuery)
                                    .scroll(ScrollMode.FORWARD_ONLY);

                            while (postHistoryIterator.next()) {
                                // first, get one PostHistory entity from the SO database schema...
                                PostHistory currentPostHistoryEntity = (PostHistory) postHistoryIterator.get(0);

                                // ignore versions that don't have any content (body or title)
                                if (currentPostHistoryEntity.getText() == null || currentPostHistoryEntity.getText().length() == 0) {
                                    continue;
                                }

                                if (PostHistory.contentPostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                    // content change

                                    // extract the post blocks (text or code)...
                                    currentPostHistoryEntity.extractPostBlocks();
                                    // ...convert them into a PostVersion (our schema)...
                                    PostVersion currentPostVersion = currentPostHistoryEntity.toPostVersion(postTypeId);
                                    // ...check if post blocks have been extracted...
                                    if (currentPostVersion.getPostBlocks().size() ==  0) {
                                        logger.warning("Thread " + partition + ": " + "No post blocks extracted for PostId: " + postId + "; PostHistoryId: " + currentPostVersion.getPostHistoryId());
                                        postsVersionsWithoutBlocks.add(currentPostVersion);
                                    }
                                    // ...and write the extracted post blocks to the database
                                    currentPostVersion.insertPostBlocks(session);
                                    // extract URLs from text blocks...
                                    currentPostVersion.extractUrlsFromTextBlocks();
                                    // ...and write them to the database
                                    currentPostVersion.insertUrls(session);
                                    // finally, add this version to the post version list
                                    postVersionList.add(currentPostVersion);

                                } else if (PostHistory.titlePostHistoryTypes.contains(currentPostHistoryEntity.getPostHistoryTypeId())) {
                                    // title change
                                    if (titleVersionList != null) {
                                        titleVersionList.add(currentPostHistoryEntity.toTitleVersion(postTypeId));
                                    }
                                }
                            }

                            if (postVersionList.size() == 0) {
                                logger.warning("Thread " + partition + ": " + "No content versions extracted for PostId " + postId);
                                postsWithoutContentVersions.add(postVersionList);
                            } else {
                                postsWithContentVersionsCount++;

                                // sort post history chronologically
                                postVersionList.sort();

                                // (1) set pred and succ references for post versions
                                // (2) compute similarity for text and code blocks
                                // (3) compute diffs for similar text and code blocks
                                postVersionList.processVersionHistory();

                                // write post versions to database and update post blocks
                                postVersionList.insert(session);
                            }

                            if (postTypeId == Posts.QUESTION_ID && titleVersionList.size() == 0) {
                                logger.warning("Thread " + partition + ": " + "No title versions extracted for PostId " + postId);
                                postsWithoutTitleVersions.add(titleVersionList);
                            } else {
                                if (titleVersionList != null) {
                                    // sort post history chronologically
                                    titleVersionList.sort();
                                    // (1) set pred and succ references for title versions
                                    // (2) compute edit distance between title versions
                                    titleVersionList.processVersionHistory();

                                    // write title versions to database
                                    titleVersionList.insert(session);
                                }
                            }

                            // commit transaction
                            t.commit();
                        }
                    }

                    int processedPostsCount = postsWithContentVersionsCount + postsWithoutContentVersions.size();
                    logger.info("Thread " + partition + ": " + processedPostsCount + " PostIds have been processed.");
                    if (processedPostsCount != recordCount) {
                        logger.warning("Thread " + partition + ": Processed post count does not match record count " +
                                "(records: " + recordCount + "; processed posts: " + processedPostsCount + ")");
                    }

                    logger.info("Thread " + partition + ": Writing PostIds of " + postsWithoutContentVersions.size()
                            + " posts for which no content versions have been extracted...");
                    writePostsWithoutVersionsToCSV(postsWithoutContentVersions, "_no_content_versions");

                    logger.info("Thread " + partition + ": Writing PostIds of " + postsWithoutTitleVersions.size()
                            + " post versions for which no title versions have been extracted...");
                    writePostsWithoutVersionsToCSV(postsWithoutTitleVersions, "_no_title_versions");

                    logger.info("Thread " + partition + ": Writing PostIds and PostHistoryIds of " + postsVersionsWithoutBlocks.size()
                            + " post versions for which no post blocks have been extracted...");
                    writePostVersionsWithoutBlocksToCSV(postsVersionsWithoutBlocks);

                } catch (IOException e) {
                    logger.warning(ErrorUtils.exceptionStackTraceToString(e));
                }
            } catch (RuntimeException e) {
                logger.warning(ErrorUtils.exceptionStackTraceToString(e));
                if (t != null) {
                    t.rollback();
                }
            }
        }

        private void writePostsWithoutVersionsToCSV(List<VersionList> versionLists, String filenameSuffix) {
            String filename =  baseFilename + "_" + partition + filenameSuffix + ".csv";
            File outputFile = Paths.get(dataDir.toString(), filename).toFile();
            if (outputFile.exists()) {
                if (!outputFile.delete()) {
                    throw new IllegalStateException("Error while deleting output file: " + outputFile);
                }
            }
            logger.info("Writing data to CSV file " + outputFile.getName() + " ...");
            try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), csvFormatPost)) {
                // header is automatically written
                // write postIds along with postTypeId
                for (VersionList versionList : versionLists) {
                    csvPrinter.printRecord(versionList.getPostId(), versionList.getPostTypeId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writePostVersionsWithoutBlocksToCSV(List<PostVersion> postVersions) {
            String filename =  baseFilename + "_" + partition + "_no_blocks.csv";
            File outputFile = Paths.get(dataDir.toString(), filename).toFile();
            if (outputFile.exists()) {
                if (!outputFile.delete()) {
                    throw new IllegalStateException("Error while deleting output file: " + outputFile);
                }
            }
            logger.info("Writing data to CSV file " + outputFile.getName() + " ...");
            try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), csvFormatVersion)) {
                // header is automatically written
                // write postIds along with postTypeId and postHistoryId
                for (PostVersion postVersion : postVersions) {
                    csvPrinter.printRecord(postVersion.getPostId(), postVersion.getPostTypeId(), postVersion.getPostHistoryId());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
