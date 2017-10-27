package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static de.unitrier.st.soposthistory.util.Util.processFiles;

public class GroundTruth extends LinkedList<PostBlockLifeSpanVersion> {
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

    public static GroundTruth readFromCSV(Path dir, int postId) {
        // ensure that input directory exists
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + dir);
        }

        Path pathToCSVFile = Paths.get(dir.toString(), "completed_" + postId + ".csv");
        GroundTruth gt = new GroundTruth(postId);

        try (CSVParser csvParser = new CSVParser(new FileReader(pathToCSVFile.toFile()), csvFormatGT)) {
            logger.info("Reading GT from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postIdFile = Integer.parseInt(currentRecord.get("PostId"));
                if (postIdFile != postId) {
                    throw new IllegalArgumentException("Wrong post id in GT: " + postIdFile + " instead of " + postId);
                }
                int postHistoryId = Integer.parseInt(currentRecord.get("PostHistoryId"));
                int postBlockTypeId = Integer.parseInt(currentRecord.get("PostBlockTypeId"));
                int localId = Integer.parseInt(currentRecord.get("LocalId"));
                Integer predLocalId = currentRecord.get("PredLocalId") == null ? null : Integer.parseInt(currentRecord.get("PredLocalId"));
                Integer succLocalId = currentRecord.get("SuccLocalId") == null ? null : Integer.parseInt(currentRecord.get("SuccLocalId"));
                String comment = currentRecord.get("Comment");

                gt.add(new PostBlockLifeSpanVersion(postId, postHistoryId, postBlockTypeId, localId,
                        predLocalId, succLocalId, comment));
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
                file -> GroundTruth.readFromCSV(
                            dir,
                            Integer.parseInt(file.toFile().getName()
                                    .replace("completed_", "")
                                    .replace(".csv", ""))
                )
        );
    }

    private void sort() {
        this.sort((gtBlock1, gtBlock2) -> {
            int result = Integer.compare(gtBlock1.getPostHistoryId(), gtBlock2.getPostHistoryId());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(gtBlock1.getLocalId(), gtBlock2.getLocalId());
            }
        });
    }

    public List<List<PostBlockLifeSpanVersion>> getOrderedVersion() {
        List<List<PostBlockLifeSpanVersion>> orderedVersions = new LinkedList<>();

        Map<Integer, List<PostBlockLifeSpanVersion>> versionsPerPostHistoryId = this.stream()
                .collect(Collectors.groupingBy(PostBlockLifeSpanVersion::getPostHistoryId));

        List<Integer> postHistoryIds = new LinkedList<>(versionsPerPostHistoryId.keySet());
        postHistoryIds.sort(Integer::compare);

        for (int postHistoryId : postHistoryIds) {
            List<PostBlockLifeSpanVersion> versions = versionsPerPostHistoryId.get(postHistoryId);
            orderedVersions.add(versions);
        }

        return orderedVersions;
    }

    public List<PostBlockLifeSpan> extractPostBlockLifeSpans() {
        return extractPostBlockLifeSpans(PostVersionList.PostBlockTypeFilter.BOTH);
    }

    public List<PostBlockLifeSpan> extractPostBlockLifeSpans(PostVersionList.PostBlockTypeFilter filter) {
        List<List<PostBlockLifeSpanVersion>> orderedVersions = getOrderedVersion();

        List<PostBlockLifeSpan> lifeSpans = new LinkedList<>();

        for (int i=0; i<orderedVersions.size(); i++) {

            List<PostBlockLifeSpanVersion> currentVersion = orderedVersions.get(i);

            for (PostBlockLifeSpanVersion currentLifeSpanVersion : currentVersion) {
                // apply filter
                if ((currentLifeSpanVersion.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId && filter == PostVersionList.PostBlockTypeFilter.CODE)
                        || (currentLifeSpanVersion.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId && filter == PostVersionList.PostBlockTypeFilter.TEXT)) {
                    continue;
                }

                PostBlockLifeSpan lifeSpan = new PostBlockLifeSpan(
                        currentLifeSpanVersion.getPostId(),
                        currentLifeSpanVersion.getPostBlockTypeId()
                );

                // find successor of this lifespan version
                int versionCount = 0;
                while (i+versionCount < orderedVersions.size()) {
                    // skip life span versions that have already been processed
                    if (currentLifeSpanVersion.isProcessed()) {
                        break;
                    }

                    currentLifeSpanVersion.setVersion(versionCount+1);
                    currentLifeSpanVersion.setProcessed(true);
                    lifeSpan.add(currentLifeSpanVersion);

                    // this version does not have a predecessor
                    if (currentLifeSpanVersion.getSuccLocalId() == null) {
                        break;
                    }

                    List<PostBlockLifeSpanVersion> nextVersion = orderedVersions.get(i+versionCount+1);

                    final int succLocalId = currentLifeSpanVersion.getSuccLocalId();
                    List<PostBlockLifeSpanVersion> nextLifeSpanVersionCandidates = nextVersion.stream()
                            .filter(v -> v.getLocalId() == succLocalId)
                            .collect(Collectors.toList());

                    if (nextLifeSpanVersionCandidates.size() == 0) {
                        throw new IllegalStateException("No successor found.");
                    }

                    if (nextLifeSpanVersionCandidates.size() > 1) {
                        throw new IllegalStateException("More than one successor found.");
                    }

                    PostBlockLifeSpanVersion nextLifeSpanVersion = nextLifeSpanVersionCandidates.get(0);

                    if (!currentLifeSpanVersion.getSuccLocalId().equals(nextLifeSpanVersion.getLocalId()) ||
                            !nextLifeSpanVersion.getPredLocalId().equals(currentLifeSpanVersion.getLocalId())) {
                        throw new IllegalStateException("Predecessor and Successor LocalIds do not match.");
                    }

                    currentLifeSpanVersion = nextLifeSpanVersion;
                    versionCount++;
                }

                if (lifeSpan.size() > 0) {
                    lifeSpans.add(lifeSpan);
                }
            }
        }

        int lifeSpanVersionCount = lifeSpans.stream()
                .map(List::size)
                .mapToInt(Integer::intValue)
                .sum();

        if (lifeSpanVersionCount != this.size()) {
            throw new IllegalStateException("There number of lifespan versions differs from the number of verions in the ground truth.");
        }

        return lifeSpans;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("GroundTruth for PostId " + postId + ":\n");
        for (PostBlockLifeSpanVersion version : this) {
            result.append(version);
            result.append("\n");
        }
        return result.toString();
    }

}
