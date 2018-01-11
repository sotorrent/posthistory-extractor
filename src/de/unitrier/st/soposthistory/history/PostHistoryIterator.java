package de.unitrier.st.soposthistory.history;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.diffs.PostBlockDiff;
import de.unitrier.st.soposthistory.urls.PostVersionUrl;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.apache.commons.csv.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PostHistoryIterator {

    private static Logger logger = null;
    private static final CSVFormat csvFormat;
    private static final int LOG_PACE = 1000;
    public static SessionFactory sessionFactory = null;

    private File dataDir;
    private String baseFilename;
    private int partitionCount;
    private String[] tags;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(PostHistoryIterator.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for in- and output
        csvFormat = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId")
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
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), csvFormat)) {
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

        try (CSVParser csvParser = new CSVParser(new FileReader(inputFile), csvFormat.withFirstRecordAsHeader())) {
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
        subLists = split(postIds);
        for (int i=0; i<subLists.length; i++) {
            List<Integer> list = subLists[i];
            writePostIdsToCSV(list, 1, baseFilename + "_questions_" + i + ".csv");
        }
        logger.info("Splitting of questions complete.");

        // split answers
        logger.info("Splitting answers...");
        postIds = readPostIdsFromCSV(baseFilename + "_answers.csv");
        subLists = split(postIds);
        for (int i=0; i<subLists.length; i++) {
            List<Integer> list = subLists[i];
            writePostIdsToCSV(list, 2, baseFilename + "_answers_" + i + ".csv");
        }
        logger.info("Splitting of answers complete.");
    }

    private List<Integer>[] split(List<Integer> postIds) {
        int partitionSize = (int)Math.ceil((double)postIds.size()/partitionCount);
        @SuppressWarnings("unchecked")
        List<Integer>[] subLists = new List[partitionCount];
        int subListIndex = 0;
        for (int i=0; i<postIds.size(); i+=partitionSize) {
            subLists[subListIndex] = postIds.subList(i, Math.min(postIds.size(), i+partitionSize));
            subListIndex++;
        }
        return subLists;
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
        private final String filename;
        private final int partition;

        ExtractionThread(String baseFilename, int partition) {
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

            // read all PostIds from the CSV file and extract history from table PostHistory
            Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
            try (StatelessSession session = sessionFactory.openStatelessSession()) {
                try (CSVParser csvParser = new CSVParser(
                        new FileReader(inputFile), csvFormat.withFirstRecordAsHeader())) {

                    // read all records into memory
                    List<CSVRecord> records = csvParser.getRecords();
                    int recordCount = records.size();

                    logger.info("Thread " + partition + ": " + recordCount + " posts read.");

                    // iterate over records
                    for (int i=0; i<recordCount; i++) {
                        CSVRecord record = records.get(i);
                        int postId = Integer.parseInt(record.get("PostId"));
                        int postTypeId = Integer.parseInt(record.get("PostTypeId"));

                        // log only every LOG_PACE record
                        if (i == 0 || i == recordCount-1 || i % LOG_PACE == 0) {
                            // Locale.ROOT -> force '.' as decimal separator
                            String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(i+1))/recordCount*100));
                            logger.info( "Thread " + partition + ": Current PostId: " + postId
                                    + " (PostTypeId: " + postTypeId
                                    + "; record " + (i+1) + " of " + recordCount + "; " + progress + ")");
                        }

                        if (postTypeId == 1 || postTypeId == 2) { // question or answer
                            // retrieve data from post history...
                            PostVersionList postVersionList = new PostVersionList(postId, postTypeId);

                            // get all PostHistory entries for current PostId, order them chronologically
                            String currentPostHistoryQuery = String.format("FROM PostHistory " +
                                    "WHERE PostId=%d AND postHistoryTypeId IN (%s)",
                                    postId, PostHistory.getRelevantPostHistoryTypes()
                            );

                            t = session.beginTransaction();

                            ScrollableResults postHistoryIterator = session.createQuery(currentPostHistoryQuery)
                                    .scroll(ScrollMode.FORWARD_ONLY);
                            while (postHistoryIterator.next()) {
                                // first, get one PostHistory entity from the SO database schema...
                                PostHistory currentPostHistoryEntity = (PostHistory)postHistoryIterator.get(0);
                                // ...extract the post blocks (text or code)...
                                currentPostHistoryEntity.extractPostBlocks();
                                // ...convert them into a PostVersion (our schema)...
                                currentPostHistoryEntity.setPostTypeId(postTypeId);
                                PostVersion currentPostVersion = currentPostHistoryEntity.toPostVersion();
                                // ...and write the extracted post blocks to the database
                                currentPostVersion.insertPostBlocks(session);
                                // extract URLs from text blocks...
                                currentPostVersion.extractUrlsFromTextBlocks();
                                // ...and write them to the database
                                currentPostVersion.insertUrls(session);
                                // finally, add this version to the post version list
                                postVersionList.add(currentPostVersion);
                            }

                            // sort post history chronologically (according to post history id)
                            postVersionList.sort();

                            // (1) set pred and succ references for post versions
                            // (2) compute similarity for text and code blocks
                            // (3) compute diffs for similar text and code blocks
                            postVersionList.processVersionHistory();

                            // write post history versions to database and update post blocks
                            postVersionList.insert(session);

                            // commit transaction
                            t.commit();
                        }
                    }

                    logger.info("Thread " + partition + ": All PostIds have been processed.");

                } catch (IOException e) {
                    logger.warning(Util.exceptionStackTraceToString(e));
                }
            } catch (RuntimeException e) {
                logger.warning(Util.exceptionStackTraceToString(e));
                if (t != null) {
                    t.rollback();
                }
            }
        }
    }
}
