package de.unitrier.st.soposthistory;

import de.unitrier.st.soposthistory.history.PostHistoryList;
import org.apache.commons.cli.*;

import java.nio.file.Path;
import java.nio.file.Paths;

class MainSample {

    public static void main (String[] args) {
        System.out.println("SOPostHistory (Sampling Mode)");

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
            commandLineFormatter.printHelp("SOPostHistory (Sampling Mode)", options);
            System.exit(1);
            return;
        }

        Path dataDirPath = Paths.get(commandLine.getOptionValue("data-dir"));
        Path hibernateConfigFilePath = Paths.get(commandLine.getOptionValue("hibernate-config"));

        PostHistoryList.createSessionFactory(hibernateConfigFilePath);

        PostHistoryList postHistoryList = new PostHistoryList(3758880, 2);
        postHistoryList.retrieveFromDatabase();
        postHistoryList.writeToCSV(dataDirPath);

        PostHistoryList.sessionFactory.close();
    }
}
