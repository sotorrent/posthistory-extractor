package de.unitrier.st.soposthistory.gt;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static de.unitrier.st.soposthistory.util.Util.processFiles;

public class GroundTruth extends LinkedList<GroundTruthBlock> {
    private static Logger logger = null;
    private static final CSVFormat csvFormatGT;

    private int postId;

    static {
        // configure logger
        try {
            logger = getClassLogger(GroundTruth.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for ground truth
        csvFormatGT = CSVFormat.DEFAULT
                .withHeader("PostId", "PostHistoryId", "PostBlockTypeId", "LocalId", "PredLocalId", "SuccLocalId", "Comment")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null")
                .withFirstRecordAsHeader();
    }

    public GroundTruth(int postId) {
        this.postId = postId;
    }

    public static GroundTruth readFromCSV(Path csvFile) {
        // ensure that input file exists
        if (!Files.exists(csvFile)) {
            throw new IllegalArgumentException("CSV file not found: " + csvFile);
        }

        logger.info("Reading GT from CSV file " + csvFile.getFileName() + " ...");

        int gtPostId = Integer.parseInt(csvFile.toFile().getName()
                .replace("completed_", "")
                .replace(".csv", ""));

        GroundTruth gt = new GroundTruth(gtPostId);

        try (CSVParser csvParser = new CSVParser(new FileReader(csvFile.toFile()), csvFormatGT)) {
            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                if (postId != gtPostId) {
                    throw new IllegalArgumentException("Wrong post id in GT: " + postId + " instead of " + gtPostId);
                }
                int postHistoryId = Integer.parseInt(currentRecord.get("PostHistoryId"));
                int postBlockTypeId = Integer.parseInt(currentRecord.get("PostBlockTypeId"));
                int localId = Integer.parseInt(currentRecord.get("LocalId"));
                Integer predLocalId = currentRecord.get("PredLocalId") == null ? null : Integer.parseInt(currentRecord.get("PredLocalId"));
                Integer succLocalId = currentRecord.get("SuccLocalId") == null ? null : Integer.parseInt(currentRecord.get("SuccLocalId"));
                String comment = currentRecord.get("Comment");

                gt.add(new GroundTruthBlock(postId, postHistoryId, postBlockTypeId, localId, predLocalId, succLocalId, comment));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        gt.sort();

        return gt;
    }

    public static List<GroundTruth> readFromDirectory(Path dir) {
        return processFiles(dir,
                file -> file.getFileName().toString().startsWith("completed_") && file.getFileName().toString().endsWith(".csv"),
                GroundTruth::readFromCSV
        );
    }

    public void sort() {
        this.sort((gtBlock1, gtBlock2) -> {
            int result = Integer.compare(gtBlock1.getPostHistoryId(), gtBlock2.getPostHistoryId());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(gtBlock1.getLocalId(), gtBlock2.getLocalId());
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("GroundTruth for PostId " + postId + ":\n");
        for (GroundTruthBlock gt : this) {
            result.append(gt);
            result.append("\n");
        }
        return result.toString();
    }

}
