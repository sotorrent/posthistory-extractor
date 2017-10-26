package de.unitrier.st.soposthistory.util;

import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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

    public static Logger getClassLogger(Class c, boolean consoleOutput) throws IOException {
        Path logFileDir  = Paths.get(System.getProperty("user.dir"));
        String logFile = Paths.get(logFileDir.toString(), c.getSimpleName() + ".log").toString();
        Logger logger = Logger.getLogger(c.getName());
        if (!consoleOutput) {
            logger.setUseParentHandlers(false); // disable handlers inherited from root logger
        }
        Handler fileHandler = new FileHandler(logFile);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        return logger;
    }
}
