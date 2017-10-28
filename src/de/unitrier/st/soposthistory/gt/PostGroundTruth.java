package de.unitrier.st.soposthistory.gt;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;
import static de.unitrier.st.soposthistory.util.Util.processFiles;

public class PostGroundTruth extends LinkedList<PostBlockLifeSpanVersion> {
    private static Logger logger = null;
    private static final CSVFormat csvFormatGT;

    private int postId;
    private List<List<PostBlockLifeSpanVersion>> orderedByVersion;

    static {
        // configure logger
        try {
            logger = getClassLogger(PostGroundTruth.class, false);
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

    public PostGroundTruth(int postId) {
        this.postId = postId;
        this.orderedByVersion = null;
    }

    public static PostGroundTruth readFromCSV(Path dir, int postId) {
        // ensure that input directory exists
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Directory does not exist: " + dir);
        }

        Path pathToCSVFile = Paths.get(dir.toString(), "completed_" + postId + ".csv");
        PostGroundTruth gt = new PostGroundTruth(postId);

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
        gt.orderByVersions();

        return gt;
    }

    public static List<PostGroundTruth> readFromDirectory(Path dir) {
        return processFiles(dir,
                file -> file.getFileName().toString().startsWith("completed_") && file.getFileName().toString().endsWith(".csv"),
                file -> PostGroundTruth.readFromCSV(
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

    private void orderByVersions() {
        orderedByVersion = new LinkedList<>();

        Map<Integer, List<PostBlockLifeSpanVersion>> versionsPerPostHistoryId = this.stream()
                .collect(Collectors.groupingBy(PostBlockLifeSpanVersion::getPostHistoryId));

        List<Integer> postHistoryIds = new LinkedList<>(versionsPerPostHistoryId.keySet());
        postHistoryIds.sort(Integer::compare);

        for (int postHistoryId : postHistoryIds) {
            List<PostBlockLifeSpanVersion> versions = versionsPerPostHistoryId.get(postHistoryId);
            orderedByVersion.add(versions);
        }
    }

    public List<PostBlockLifeSpan> extractPostBlockLifeSpans() {
        return extractPostBlockLifeSpans(Sets.newHashSet(TextBlockVersion.postBlockTypeId, CodeBlockVersion.postBlockTypeId));
    }

    public List<PostBlockLifeSpan> extractPostBlockLifeSpans(Set<Integer> postBlockTypeFilter) {
        List<PostBlockLifeSpan> postBlockLifeSpans = new LinkedList<>();

        for (int i = 0; i< orderedByVersion.size(); i++) {
            List<PostBlockLifeSpanVersion> currentVersion = orderedByVersion.get(i);

            for (PostBlockLifeSpanVersion currentLifeSpanVersion : currentVersion) {
                // apply filter
                if (!currentLifeSpanVersion.isSelected(postBlockTypeFilter)) {
                    continue;
                }

                PostBlockLifeSpan lifeSpan = new PostBlockLifeSpan(
                        currentLifeSpanVersion.getPostId(),
                        currentLifeSpanVersion.getPostBlockTypeId()
                );

                // find successor of this lifespan version
                int versionCount = 0;
                while (i+versionCount < orderedByVersion.size()) {
                    // skip life span versions that have already been processed
                    if (currentLifeSpanVersion.isProcessed()) {
                        break;
                    }

                    currentLifeSpanVersion.setProcessed(true);
                    lifeSpan.add(currentLifeSpanVersion);

                    // this version does not have a predecessor
                    if (currentLifeSpanVersion.getSuccLocalId() == null) {
                        break;
                    }

                    List<PostBlockLifeSpanVersion> nextVersion = orderedByVersion.get(i+versionCount+1);

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
                    postBlockLifeSpans.add(lifeSpan);
                }
            }
        }

        int lifeSpanVersionCount = postBlockLifeSpans.stream()
                .map(List::size)
                .mapToInt(Integer::intValue)
                .sum();

        if ((postBlockTypeFilter.equals(Sets.newHashSet(TextBlockVersion.postBlockTypeId, CodeBlockVersion.postBlockTypeId)) && lifeSpanVersionCount != this.size())
                || (postBlockTypeFilter.equals(Sets.newHashSet(TextBlockVersion.postBlockTypeId)) && lifeSpanVersionCount != this.getTextBlocks().size())
                || (postBlockTypeFilter.equals(Sets.newHashSet(CodeBlockVersion.postBlockTypeId)) && lifeSpanVersionCount != this.getCodeBlocks().size())) {
            throw new IllegalStateException("The number of lifespan versions differs from the number of versions in the ground truth.");
        }

        return postBlockLifeSpans;
    }

    public List<PostBlockLifeSpanVersion> getTextBlocks() {
        return this.stream()
                .filter(v -> v.getPostBlockTypeId() == TextBlockVersion.postBlockTypeId)
                .collect(Collectors.toList());
    }

    public List<PostBlockLifeSpanVersion> getCodeBlocks() {
        return this.stream()
                .filter(v -> v.getPostBlockTypeId() == CodeBlockVersion.postBlockTypeId)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("PostGroundTruth for PostId " + postId + ":\n");
        for (PostBlockLifeSpanVersion version : this) {
            result.append(version);
            result.append("\n");
        }
        return result.toString();
    }

}
