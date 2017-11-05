package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// TODO: revise and move to metrics comparison project
public class Statistics {

    public static void main(String[] args){
        Path pathToPossibleMultipleConnections = Paths.get("testdata", "metrics comparison", "possible multiple connections.csv");
        Path pathToDirectoryWithPossibleMultipleConnections = Paths.get("testdata", "Samples_possibleMultipleConnections");

        Statistics statistics = new Statistics();
        statistics.findPostsWithPossibleMultipleConnections(pathToPossibleMultipleConnections);
        statistics.copyPostsWithPossibleMultipleConnectionsIntoOwnDirectory(
                pathToPossibleMultipleConnections,
                pathToDirectoryWithPossibleMultipleConnections);
    }

    List<String> pathToAllDirectories = new LinkedList<>();

    private Statistics(){
        for(int i=1; i<=10; i++) {
            pathToAllDirectories.add(Paths.get("testdata","Samples_10000", "PostId_VersionCount_SO_17-06_sample_10000_" + i, "files").toString());
        }
        // TODO: remove this later. Only for testing purposes:
        // pathToAllDirectories.add(Paths.get("testdata").toString());
    }


    private void findPostsWithPossibleMultipleConnections(Path pathToWriteFile) {

        StringBuilder output = new StringBuilder();

        output.append("postId;postHistoryId;localId;blockTypeId;possiblePredOrSuccLocalIds;numberOfPossibleSuccessorsOrPredecessors\n");

        for (String path : pathToAllDirectories) {
            List<PostVersionList> postVersionLists = PostVersionList.readFromDirectory(Paths.get(path));

            for (PostVersionList postVersionList : postVersionLists) {
                for (int j = 0; j < postVersionList.size(); j++) {
                    if (j > 0) {
                        for (int k = 0; k < postVersionList.get(j).getPostBlocks().size(); k++) {
                            PostBlockVersion postBlockVersion = postVersionList.get(j).getPostBlocks().get(k);
                            LinkedList<Integer> possiblePreds = new LinkedList<>();
                            for (int l = 0; l < postVersionList.get(j - 1).getPostBlocks().size(); l++) {
                                PostBlockVersion postBlockVersionPred = postVersionList.get(j - 1).getPostBlocks().get(l);

                                if (postBlockVersion.getContent().equals(postBlockVersionPred.getContent()))
                                    possiblePreds.add(postBlockVersionPred.getLocalId());
                            }

                            if (possiblePreds.size() > 1) {
                                output
                                        .append(postVersionList.getFirst().getPostId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostHistoryId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostBlocks().get(k).getLocalId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostBlocks().get(k) instanceof TextBlockVersion ? 1 : 2)
                                        .append(";")
                                        // .append("localIdsOfPossiblePreds:")
                                        .append(possiblePreds)
                                        .append(";")
                                        .append(possiblePreds.size())
                                        .append("\n");
                            }
                        }
                    }

                    if (j < postVersionList.size() - 1) {
                        for (int k = 0; k < postVersionList.get(j).getPostBlocks().size(); k++) {
                            PostBlockVersion postBlockVersion = postVersionList.get(j).getPostBlocks().get(k);
                            LinkedList<Integer> possibleSuccs = new LinkedList<>();
                            for (int l = 0; l < postVersionList.get(j + 1).getPostBlocks().size(); l++) {
                                PostBlockVersion postBlockVersionSucc = postVersionList.get(j + 1).getPostBlocks().get(l);

                                if (postBlockVersion.getContent().equals(postBlockVersionSucc.getContent()))
                                    possibleSuccs.add(postBlockVersionSucc.getLocalId());
                            }

                            if (possibleSuccs.size() > 1) {
                                output
                                        .append(postVersionList.getFirst().getPostId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostHistoryId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostBlocks().get(k).getLocalId())
                                        .append(";")
                                        .append(postVersionList.get(j).getPostBlocks().get(k) instanceof TextBlockVersion ? 1 : 2)
                                        .append(";")
                                        // .append("local-ids of possible succs: ")
                                        .append(possibleSuccs)
                                        .append(";")
                                        .append(possibleSuccs.size())
                                        .append("\n");
                            }
                        }
                    }
                }
            }

            System.out.println("Finished: " + path);
        }

        try {
            PrintWriter printWriter = new PrintWriter(pathToWriteFile.toString());
            printWriter.write(output.toString());
            printWriter.flush();
            printWriter.close();

        } catch (FileNotFoundException e) {
            System.err.println("Couldn't write file: possible multiple connections.csv");
        }
    }

    private void copyPostsWithPossibleMultipleConnectionsIntoOwnDirectory(Path pathToPossibleMultipleConnectionsFile, Path pathToDirectoryToCopy){

        // get all postIds of file possible multiple connections.csv
        List<Integer> postIdsOfAffectedPosts = new ArrayList<>();
        CSVParser csvParser;
        try {
            csvParser = CSVParser.parse(
                    pathToPossibleMultipleConnectionsFile.toFile(),
                    StandardCharsets.UTF_8,
                    MetricComparisonManager.csvFormatMetricComparison.withFirstRecordAsHeader()
            );

            csvParser.getHeaderMap();
            List<CSVRecord> records = null;

            records = csvParser.getRecords();

            for(CSVRecord record : records){
                postIdsOfAffectedPosts.add(Integer.valueOf(record.get("postId")));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        for (String pathToDirectory : pathToAllDirectories) {
            File file = new File(pathToDirectory);
            File[] allPostVersionListsInFolder = file.listFiles((dir, name) -> name.matches(PostVersionList.fileNamePattern.pattern())); // https://stackoverflow.com/questions/4852531/find-files-in-a-folder-using-java

            assert allPostVersionListsInFolder != null;
            for (File postVersionListFile : allPostVersionListsInFolder) {
                Integer tmpPostId = Integer.valueOf(postVersionListFile.getName().replace(".csv", ""));
                if (postIdsOfAffectedPosts.contains(tmpPostId)) {
                    try {
                        Files.copy(
                                postVersionListFile.toPath(),
                                Paths.get(pathToDirectoryToCopy.toString(), tmpPostId + ".csv"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
