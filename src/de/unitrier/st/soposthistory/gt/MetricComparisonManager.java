package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

// TODO: move to metrics comparison project

public class MetricComparisonManager {
    private static Logger logger = null;
    private static final CSVFormat csvFormatPostIds;

    private Map<Integer, PostGroundTruth> groundTruth;
    private Map<Integer, PostVersionList> postHistory;

    static {
        // configure logger
        try {
            logger = getClassLogger(PostGroundTruth.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for list of PostIds
        csvFormatPostIds = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId", "VersionCount")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withFirstRecordAsHeader();
    }

    private MetricComparisonManager() {
        groundTruth = new HashMap<>();
        postHistory = new HashMap<>();
    }

    public static MetricComparisonManager create(Path postIdPath, Path postHistoryPath, Path groundTruthPath) {
        // ensure that input file exists (directories are tested in read methods)
        if (!Files.exists(postIdPath) || Files.isDirectory(postIdPath)) {
            throw new IllegalArgumentException("File not found: " + postIdPath);
        }

        MetricComparisonManager manager = new MetricComparisonManager();

        try (CSVParser csvParser = new CSVParser(new FileReader(postIdPath.toFile()), csvFormatPostIds)) {
            logger.info("Reading PostIds from CSV file " + postIdPath.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                int postTypeId = Integer.parseInt(currentRecord.get("PostTypeId"));
                int versionCount = Integer.parseInt(currentRecord.get("VersionCount"));

                // read post version list
                PostVersionList postVersionList = PostVersionList.readFromCSV(
                        postHistoryPath, postId, postTypeId, false
                );

                if (postVersionList.size() != versionCount) {
                    throw new IllegalArgumentException("Version count expected to be " + versionCount
                            + ", but was " + postVersionList.size()
                    );
                }

                manager.postHistory.put(postId, postVersionList);

                // read ground truth
                PostGroundTruth postGroundTruth = PostGroundTruth.readFromCSV(groundTruthPath, postId);

                if (postGroundTruth.getPossibleConnections() != postVersionList.getPossibleConnections()) {
                    throw new IllegalArgumentException("Number of possible connections in ground truth is different" +
                            "from number of possible connections in post history.");
                }

                manager.groundTruth.put(postId, postGroundTruth);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return manager;
    }

    public Map<Integer, PostGroundTruth> getGroundTruth() {
        return groundTruth;
    }

    public Map<Integer, PostVersionList> getPostHistory() {
        return postHistory;
    }
}
