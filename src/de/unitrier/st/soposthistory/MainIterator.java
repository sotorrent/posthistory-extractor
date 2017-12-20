package de.unitrier.st.soposthistory;

import de.unitrier.st.soposthistory.history.PostHistoryIterator;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class MainIterator {
    // TODO : Also store n-grams of code blocks in database? -> would result in a very large database

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Iterator Mode)");

        Options options = new Options();

        Option dataDirOption = new Option("d", "data-dir", true, "path to data directory (used to store post id lists");
        dataDirOption.setRequired(true);
        options.addOption(dataDirOption);

        Option hibernateConfigFileOption = new Option("h", "hibernate-config", true,
                "path to hibernate config file");
        hibernateConfigFileOption.setRequired(true);
        options.addOption(hibernateConfigFileOption);

        Option tagsOption = new Option("t", "tags", true,
                "tags for filtering questions and answers (separated by a space)");
        tagsOption.setRequired(false);
        options.addOption(tagsOption);

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
        String[] tags = {}; // no tags provided -> all posts

        if (commandLine.hasOption("tags")) {
            tags = commandLine.getOptionValue("tags").split(" ");
        }

        PostHistoryIterator.createSessionFactory(hibernateConfigFilePath);

        PostHistoryIterator postHistoryIterator = new PostHistoryIterator(dataDirPath, "all",
                4, tags);

        postHistoryIterator.extractAndSavePostIds(); // including split

        postHistoryIterator.extractDataFromPostHistory("questions");
        postHistoryIterator.extractDataFromPostHistory("answers");

        PostHistoryIterator.sessionFactory.close();
    }
}
