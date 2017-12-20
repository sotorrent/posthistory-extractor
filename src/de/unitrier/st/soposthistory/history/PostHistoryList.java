package de.unitrier.st.soposthistory.history;

import de.unitrier.st.util.Util;
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
import java.util.logging.Logger;

public class PostHistoryList extends LinkedList<PostHistory> {

    private static Logger logger = null;
    private static final CSVFormat outputCSVFormat;
    private static final CSVFormat inputCSVFormat;
    public static SessionFactory sessionFactory = null;

    private int postId;
    private int postTypeId;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(PostHistoryList.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for input
        inputCSVFormat = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId", "VersionCount")
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
        if (!Files.exists(hibernateConfigFilePath) || Files.isDirectory(hibernateConfigFilePath)) {
            throw new IllegalArgumentException("Invalid Hibernate config file: " + hibernateConfigFilePath);
        }

        sessionFactory = new Configuration()
                .addAnnotatedClass(PostHistory.class)
                .configure(hibernateConfigFilePath.toFile())
                .buildSessionFactory();
    }

    public static void readRetrieveAndWrite(Path inputFile, Path outputDir) {
        try {
            Util.ensureFileExists(inputFile);
            Util.ensureEmptyDirectoryExists(outputDir);

            logger.info("Reading file " + inputFile.getFileName() + " ...");

            try (CSVParser csvParser = new CSVParser(new FileReader(inputFile.toFile()), inputCSVFormat)) {

                for (CSVRecord currentRecord : csvParser) {
                    int postId = Integer.parseInt(currentRecord.get("PostId"));
                    int postTypeId = Integer.parseInt(currentRecord.get("PostTypeId"));

                    PostHistoryList currentPostHistoryList = new PostHistoryList(postId, postTypeId);
                    currentPostHistoryList.retrieveFromDatabase();
                    currentPostHistoryList.writeToCSV(outputDir);
                }
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

                // escape back slashes, in particular replace \" with \\", because the former will be exported as \"",
                // which will lead to an IOException when the CSV file is imported again
                // ("java.io.IOException: (line ...) invalid char between encapsulated token and delimiter")
                // see post 10049438 and corresponding test case in PostVersionHistoryTest
                currentPostHistoryEntity.setText(currentPostHistoryEntity.getText().replace("\\", "\\\\"));

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
