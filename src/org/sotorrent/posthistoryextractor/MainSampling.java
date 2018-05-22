package org.sotorrent.posthistoryextractor;

import org.sotorrent.posthistoryextractor.history.PostHistoryList;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Retrieve the version history for sampled posts (see project so-posthistory-gt).
 */
class MainSampling {

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Sampling Mode)");

        Options options = new Options();

        Option inputFile = new Option("i", "input-file", true, "path to input file");
        inputFile.setRequired(true);
        options.addOption(inputFile);

        Option outputDir = new Option("o", "output-dir", true, "path to output directory");
        outputDir.setRequired(true);
        options.addOption(outputDir);

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
            commandLineFormatter.printHelp("SOPostHistory (Sampling Mode)", options);
            System.exit(1);
            return;
        }

        Path inputFilePath = Paths.get(commandLine.getOptionValue("input-file"));
        Path outputDirPath = Paths.get(commandLine.getOptionValue("output-dir"));
        Path hibernateConfigFilePath = Paths.get(commandLine.getOptionValue("hibernate-config"));

        PostHistoryList.createSessionFactory(hibernateConfigFilePath);

        PostHistoryList.readRetrieveAndWrite(inputFilePath, outputDirPath);

        PostHistoryList.sessionFactory.close();
    }
}
