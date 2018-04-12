package de.unitrier.st.soposthistory.comments;

import de.unitrier.st.soposthistory.urls.CommentUrl;
import de.unitrier.st.util.Util;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Logger;

public class CommentsIterator {

    private static Logger logger = null;
    private static final int LOG_PACE = 1000;
    private static SessionFactory sessionFactory = null;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(CommentsIterator.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createSessionFactory(Path hibernateConfigFilePath) {
        if (!Files.exists(hibernateConfigFilePath) || Files.isDirectory(hibernateConfigFilePath)) {
            throw new IllegalArgumentException("Not a valid Hibernate config file: " + hibernateConfigFilePath);
        }

        sessionFactory = new Configuration()
                .addAnnotatedClass(Comments.class)
                .addAnnotatedClass(CommentUrl.class)
                .configure(hibernateConfigFilePath.toFile())
                .buildSessionFactory();
    }

    public void extractUrlsFromComments() {
        if (sessionFactory == null) {
            throw new IllegalStateException("Static session factory not created yet.");
        }

        logger.info("Starting extraction of URLs from comments...");

        long processedComments = 0;

        Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            t = session.beginTransaction();

            long commentCount = (long) session.createQuery("SELECT COUNT(*) FROM Comments").getSingleResult();

            logger.info("Processing " + commentCount + " comments...");

            ScrollableResults commentsIterator = session.createQuery("FROM Comments")
                    .scroll(ScrollMode.FORWARD_ONLY);

            while (commentsIterator.next()) {
                processedComments++;

                // retrieve next comment
                Comments currentComment = (Comments) commentsIterator.get(0);
                // extract URLs
                currentComment.extractUrls();
                // add URLs to database
                currentComment.insertUrls(session);

                // log only every LOG_PACE record
                if (processedComments == 1 || processedComments == commentCount || processedComments % LOG_PACE == 0) {
                    // Locale.ROOT -> force '.' as decimal separator
                    String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(processedComments))/commentCount*100));
                    logger.info( "Current CommentId: " + currentComment.getId()
                            + "; record " + processedComments + " of " + commentCount + "; " + progress + ")");
                }
            }

            // commit transaction
            t.commit();

            logger.info("Extraction of URLs from comments finished...");

        } catch (RuntimeException e) {
            logger.warning(Util.exceptionStackTraceToString(e));
            if (t != null) {
                t.rollback();
            }
        }
    }
}
