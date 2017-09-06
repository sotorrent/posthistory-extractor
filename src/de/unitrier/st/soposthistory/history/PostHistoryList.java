package de.unitrier.st.soposthistory.history;

import org.apache.commons.csv.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PostHistoryList extends LinkedList<PostHistory> {
    // TODO: Merge with PostHistoryIterator?

    private static final Path logFileDir  = Paths.get(System.getProperty("user.dir"));
    private static final CSVFormat outputCSVFormat;
    private static final CSVFormat inputCSVFormat;

    private static Logger logger = null;
    public static SessionFactory sessionFactory = null;

    private int postId;
    private int postTypeId;

    static {
        // ensure that log dir exists
        try {
            if (!Files.exists(logFileDir)) {
                Files.createDirectory(logFileDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure logger
        try {
            String logFile = Paths.get(logFileDir.toString(), PostHistoryList.class.getSimpleName() + ".log" )
                    .toString();
            logger = Logger.getLogger(PostHistoryList.class.getName());
            Handler fileHandler = new FileHandler(logFile);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for input
        inputCSVFormat = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("")
                .withFirstRecordAsHeader();

        // configure CSV format for output
        outputCSVFormat = CSVFormat.DEFAULT
                .withHeader("Id", "PostId", "UserId", "PostHistoryTypeId", "RevisionGUID", "CreationDate", "Text",
                        "UserDisplayName", "Comment")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("");
    }

    private PostHistoryList(int postId, int postTypeId) {
        this.postId = postId;
        this.postTypeId = postTypeId;
    }

    public static void createSessionFactory(Path hibernateConfigFilePath) {
        if (!Files.exists(hibernateConfigFilePath)) {
            throw new IllegalArgumentException("Hibernate config file not found: " + hibernateConfigFilePath);
        }

        sessionFactory = new Configuration()
                .addAnnotatedClass(PostHistory.class)
                .configure(hibernateConfigFilePath.toFile())
                .buildSessionFactory();
    }

    public static void readFromCSVAndRetrieve(Path inputFile, Path outputDir) {
        // ensure that input file exists
        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Input file not found: " + inputFile);
        }

        // ensure that output dir exists, but is empty
        try {
            Files.deleteIfExists(outputDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Reading file " + inputFile.getFileName() + " ...");

        try (CSVParser csvParser = new CSVParser(new FileReader(inputFile.toFile()), inputCSVFormat)) {

            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                int postTypeId = Integer.parseInt(currentRecord.get("PostTypeId"));

                PostHistoryList currentPostHistoryList = new PostHistoryList(postId, postTypeId);
                currentPostHistoryList.retrieveFromDatabase();
                currentPostHistoryList.writeToCSV(outputDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void retrieveFromDatabase() {
        if (sessionFactory == null) {
            throw new IllegalStateException("Static session factory not created yet.");
        }

        Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            // retrieve history for post id
            t = session.beginTransaction();
            logger.info("Retrieving history for PostId: " + postId + " (PostTypeId: " + postTypeId + ")");

            String postHistoryQueryString = String.format("FROM PostHistory WHERE PostId=%d " +
                            "AND postHistoryTypeId IN (%s) ORDER BY Id ASC",
                    postId, PostHistory.getRelevantPostHistoryTypes()
            );
            ScrollableResults postHistoryIterator = session.createQuery(postHistoryQueryString)
                    .scroll(ScrollMode.FORWARD_ONLY);
            int count=0;
            while (postHistoryIterator.next()) {
                PostHistory currentPostHistoryEntity = (PostHistory) postHistoryIterator.get(0);
                this.add(currentPostHistoryEntity);
                count++;
            }
            t.commit();
            logger.info(count + " post versions retrieved.");
        } catch (RuntimeException e) {
            if (t != null) {
                t.rollback();
                e.printStackTrace();
            }
        }
    }

    private void writeToCSV(Path outputDir) {
        // create output file
        String filename = postId + ".csv";
        File outputFile = Paths.get(outputDir.toString(), filename).toFile();
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFile);
            }
        }

        // write post history
        logger.info("Writing post history to CSV file " + outputFile.getName() + " ...");
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), outputCSVFormat)) {
            // header is automatically written
            // write postIds along with postTypeId
            for (PostHistory currentPostHistory : this) {
                csvPrinter.printRecord(currentPostHistory.getId(), currentPostHistory.getPostId(),
                        currentPostHistory.getUserId(), currentPostHistory.getPostHistoryTypeId(),
                        currentPostHistory.getRevisionGuid(), currentPostHistory.getCreationDate(),
                        currentPostHistory.getText(), currentPostHistory.getUserDisplayName(),
                        currentPostHistory.getComment()
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
