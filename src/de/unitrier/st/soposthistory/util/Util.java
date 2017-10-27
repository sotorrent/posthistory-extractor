package de.unitrier.st.soposthistory.util;

import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Util {
    public static void insertList(StatelessSession session, List list) {
        for (int i=0; i<list.size(); i++) {
            session.insert(list.get(i));
        }
    }

    public static void updateList(StatelessSession session, List list) {
        for (int i=0; i<list.size(); i++) {
            session.update(list.get(i));
        }
    }

    public static Logger getClassLogger(Class c) throws IOException {
        return getClassLogger(c, true, Paths.get(System.getProperty("user.dir")));
    }

    public static Logger getClassLogger(Class c, Path logFileDir) throws IOException {
        return getClassLogger(c, true, logFileDir);
    }

    public static Logger getClassLogger(Class c, boolean consoleOutput) throws IOException {
        return getClassLogger(c, consoleOutput, Paths.get(System.getProperty("user.dir")));
    }

    public static Logger getClassLogger(Class c, boolean consoleOutput, Path logFileDir) throws IOException {
        // ensure that log dir exists
        try {
            if (!Files.exists(logFileDir)) {
                Files.createDirectory(logFileDir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String logFile = Paths.get(logFileDir.toString(), c.getSimpleName() + ".log").toString();

        // configure logger
        Logger logger = Logger.getLogger(c.getName());
        if (!consoleOutput) {
            logger.setUseParentHandlers(false); // disable handlers inherited from root logger
        }
        Handler fileHandler = new FileHandler(logFile);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);

        return logger;
    }

    public static<T> List<T> visitFiles(Path dir, Function<Path, T> visitor) {
        // switch to Java 8 Files.list()?

        // ensure that input directory exists
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory not found: " + dir);
        }

        List<T> list = new LinkedList<>();

        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    T result = visitor.apply(file);
                    if (result != null) {
                        list.add(result);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }
}
