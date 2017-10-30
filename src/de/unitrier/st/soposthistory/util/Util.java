package de.unitrier.st.soposthistory.util;

import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

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

    public static<T> List<T> processFiles(Path dir, Predicate<Path> filter, Function<Path, T> map) {
        // ensure that input directory exists
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory not found: " + dir);
        }

        try {
            return Files.list(dir)
                        .filter(filter)
                        .map(map)
                        .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}
