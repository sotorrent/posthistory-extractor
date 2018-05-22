package org.sotorrent.posthistoryextractor.comments;

import org.sotorrent.posthistoryextractor.urls.CommentUrl;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.sotorrent.util.LogUtils;
import org.sotorrent.util.collections.CollectionUtils;
import org.sotorrent.util.exceptions.ErrorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class CommentsIterator {

    private static Logger logger = null;
    private static final int LOG_PACE = 10000;
    public static SessionFactory sessionFactory = null;

    private int partitionCount;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(CommentsIterator.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CommentsIterator(int partitionCount) {
        this.partitionCount = partitionCount;
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

        Transaction t = null; // see https://docs.jboss.org/hibernate/orm/3.3/reference/en/html/transactions.html
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            @SuppressWarnings("unchecked") // see https://stackoverflow.com/a/509115
            List<Integer> commentIds = session.createQuery("SELECT id FROM Comments ORDER BY id").list();
            List<Integer>[] partitions = CollectionUtils.split(commentIds, partitionCount);

            for (int i=0; i<partitions.length; i++) {
                List<Integer> partition = partitions[i];
                int minId = partition.get(0);
                int maxId = partition.get(partition.size()-1);

                t = session.beginTransaction();

                @SuppressWarnings("unchecked") // see https://stackoverflow.com/a/509115
                List<Comments> comments = session.createQuery(String.format(
                        "FROM Comments WHERE id >= %d and id <= %d", minId, maxId)
                ).list();

                logger.info("Processing " + comments.size() + " comments in partition " + i + " ...");

                for (int j=0; j<comments.size()-1; j++) {
                    // retrieve next comment
                    Comments currentComment = comments.get(j);
                    // extract URLs
                    currentComment.extractUrls();
                    // add URLs to database
                    currentComment.insertUrls(session);

                    // log only every LOG_PACE record
                    if (j == 0 || j == comments.size()-1 || j % LOG_PACE == 0) {
                        // Locale.ROOT -> force '.' as decimal separator
                        String progress = String.format(Locale.ROOT, "%.2f%%", (((double)(j+1))/comments.size()*100));
                        logger.info( "Current CommentId: " + currentComment.getId()
                                + "; record " + j + " of " + comments.size() + "; " + progress + ")");
                    }
                }

                // commit transaction
                t.commit();

                logger.info("Extraction of URLs from comments in partition " + i + " finished...");
            }

            logger.info("Extraction of URLs from comments in all partitions finished...");

        } catch (RuntimeException e) {
            logger.warning(ErrorUtils.exceptionStackTraceToString(e));
            if (t != null) {
                t.rollback();
            }
        }
    }
}
