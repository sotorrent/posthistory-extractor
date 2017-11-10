package de.unitrier.st.soposthistory.util;

import org.apache.commons.io.FileUtils;
import org.hibernate.StatelessSession;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static Path defaultLogDir = Paths.get(System.getProperty("user.dir"), "logs");
    private static final double EPSILON = 0.00001;

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
        return getClassLogger(c, true, defaultLogDir);
    }

    public static Logger getClassLogger(Class c, Path logFileDir) throws IOException {
        return getClassLogger(c, true, logFileDir);
    }

    public static Logger getClassLogger(Class c, boolean consoleOutput) throws IOException {
        return getClassLogger(c, consoleOutput, defaultLogDir);
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

    // beginIndex inclusive, endIndex exclusive
    public static String replaceStringAt(String str, int beginIndex, int endIndex, String replacement) {
        return str.substring(0, beginIndex) + replacement + str.substring(endIndex, str.length());
    }

    public static void ensureFileExists(Path file) {
        // ensure that file exists
        if (!Files.exists(file) || Files.isDirectory(file)) {
            throw new IllegalArgumentException("File not found: " + file);
        }
    }

    public static void ensureEmptyDirectoryExists(Path dir) throws IOException {
        // ensure that output dir exists, but is empty
        if (Files.exists(dir)) {
            if (Files.isDirectory(dir)) {
                FileUtils.deleteDirectory(dir.toFile());
            } else {
                throw new IllegalArgumentException("Not a directory.");
            }
        }
        Files.createDirectories(dir);
    }

    // see http://www.cygnus-software.com/papers/comparingfloats/Comparing%20floating%20point%20numbers.htm
    public static boolean equals(double value1, double value2) {
        return equals(value1, value2, EPSILON);
    }

    private static boolean equals(double value1, double value2, double epsilon) {
        return Math.abs(value1  - value2) < epsilon;
    }

    public static boolean lessThan(double value1, double value2) {
        return lessThan(value1, value2, EPSILON);
    }

    private static boolean lessThan(double value1, double value2, double epsilon) {
        return (value2 - value1) - epsilon > 0;
    }

    public static boolean greaterThan(double value1, double value2) {
        return greaterThan(value1, value2, EPSILON);
    }

    private static boolean greaterThan(double value1, double value2, double epsilon) {
        return (value2 - value1) + epsilon < 0;
    }

    // see http://nadeausoftware.com/articles/2008/03/java_tip_how_get_cpu_and_user_time_benchmarking

    /** Get CPU time in nanoseconds. */
    public long getCpuTime( ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        return bean.isCurrentThreadCpuTimeSupported( ) ?
                bean.getCurrentThreadCpuTime( ) : 0L;
    }

    /** Get user time in nanoseconds. */
    public long getUserTime( ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        return bean.isCurrentThreadCpuTimeSupported( ) ?
                bean.getCurrentThreadUserTime( ) : 0L;
    }

    /** Get system time in nanoseconds. */
    public long getSystemTime( ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        return bean.isCurrentThreadCpuTimeSupported( ) ?
                (bean.getCurrentThreadCpuTime( ) - bean.getCurrentThreadUserTime( )) : 0L;
    }
}
