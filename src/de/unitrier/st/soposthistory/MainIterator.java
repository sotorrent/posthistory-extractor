package de.unitrier.st.soposthistory;

import de.unitrier.st.soposthistory.history.PostHistoryIterator;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class MainIterator {
    // TODO : Also store n-grams of code blocks in database?
    // TODO: add tags as command-line parameter

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Iterator Mode)");

        Options options = new Options();

        Option dataDir = new Option("d", "data-dir", true, "path to data directory");
        dataDir.setRequired(true);
        options.addOption(dataDir);

        Option hibernateConfigFile = new Option("h", "hibernate-config", true,
                "path to hibernate config file");
        hibernateConfigFile.setRequired(true);
        options.addOption(hibernateConfigFile);


        CommandLineParser commandLineParser = new DefaultParser();
        HelpFormatter commandLineFormatter = new HelpFormatter();
        CommandLine commandLine;

        try {
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            commandLineFormatter.printHelp("SOPostHistory (Iterator Mode)", options);
            System.exit(1);
            return;
        }

        Path dataDirPath = Paths.get(commandLine.getOptionValue("data-dir"));
        Path hibernateConfigFilePath = Paths.get(commandLine.getOptionValue("hibernate-config"));

        PostHistoryIterator.createSessionFactory(hibernateConfigFilePath);

        PostHistoryIterator postHistoryIterator = new PostHistoryIterator(dataDirPath, "all",
                4, new String[]{}); // no tags provided -> all posts

        postHistoryIterator.extractAndSavePostIds(); // including split

        // TODO: process questions first?
        postHistoryIterator.extractDataFromPostHistory("answers");
        postHistoryIterator.extractDataFromPostHistory("questions");

        PostHistoryIterator.sessionFactory.close();
    }
}
