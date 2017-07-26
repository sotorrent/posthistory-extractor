package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.diffs.PostBlockDiffList;
import de.unitrier.st.soposthistory.history.PostHistory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.StatelessSession;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostVersionList extends LinkedList<PostVersion> {
    private final PostBlockDiffList diffs;

    public PostVersionList() {
        super();
        diffs = new PostBlockDiffList();
    }

    public void readFromCSV(String pathname, int postId, int postTypeId) {
        CSVParser parser;
        try {
            parser = CSVParser.parse(
                    new File(pathname + postId + ".csv"),
                    StandardCharsets.UTF_8,
                    CSVFormat.DEFAULT.withHeader().withDelimiter(';')
            );
            parser.getHeaderMap();

            List<CSVRecord> records = parser.getRecords();

            if (records.size() > 0) {
                for (CSVRecord record : records) {
                    byte postHistoryTypeId = Byte.parseByte(record.get("PostHistoryTypeId"));
                    // only consider relevant Post History Types
                    if (!PostHistory.relevantPostHistoryTypes.contains((int)postHistoryTypeId)) {
                        continue;
                    }

                    String text = record.get("Text");
                    // ignore entries where column "Text" is empty
                    if (text.isEmpty()) {
                        continue;
                    }

                    int id = Integer.parseInt(record.get("Id"));
                    assertEquals(postId, Integer.parseInt(record.get("PostId")));
                    String userId = record.get("UserId");
                    String revisionGuid = record.get("RevisionGUID");
                    Timestamp creationDate = Timestamp.valueOf(record.get("CreationDate"));
                    String userDisplayName = record.get("UserDisplayName");
                    String comment = record.get("Comment");

                    PostHistory postHistory = new PostHistory(
                            id, postId, postTypeId, userId, postHistoryTypeId, revisionGuid, creationDate,
                            text, userDisplayName, comment);
                    postHistory.extractPostBlocks();
                    PostVersion postVersion = postHistory.toPostVersion();
                    postVersion.extractUrlsFromTextBlocks();

                    this.add(postVersion);
                }
            }

            processVersionHistory();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sort() {
        this.sort((v1, v2) ->
                v1.getPostHistoryId() < v2.getPostHistoryId() ? -1 : v1.getPostHistoryId() > v2.getPostHistoryId() ? 1 : 0
        );
    }

    public void processVersionHistory() {
        for (int i=0; i<this.size(); i++) {
            PostVersion currentVersion = this.get(i);

            currentVersion.extractUrlsFromTextBlocks();

            int predIndex = i-1;
            int succIndex = i+1;

            if (predIndex == -1) {
                // current is first element
                currentVersion.setPredPostHistoryId(null);
                // the post blocks in the first version have themselves as root post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    currentPostBlock.setRootPostBlockVersionId(currentPostBlock.getId());
                }
            } else {
                currentVersion.setPredPostHistoryId(this.get(predIndex).getPostHistoryId());

                // compute similarity and diffs
                PostVersion previousVersion = this.get(i-1);
                // text blocks
                currentVersion.computePostBlockSimilarityAndDiffs(
                        currentVersion.getTextBlocks(),
                        previousVersion.getTextBlocks(),
                        TextBlockVersion.similarityThreshold
                );
                // code blocks
                currentVersion.computePostBlockSimilarityAndDiffs(
                        currentVersion.getCodeBlocks(),
                        previousVersion.getCodeBlocks(),
                        CodeBlockVersion.similarityThreshold
                );

                // set root post blocks
                for (PostBlockVersion currentPostBlock : currentVersion.getPostBlocks()) {
                    if (currentPostBlock.getPred() == null) {
                        // block has no predecessor -> set itself as root post block
                        currentPostBlock.setRootPostBlockVersionId(currentPostBlock.getId());
                    } else {
                        // block has predecessor -> set root post block of predecessor as root post block of this block
                        currentPostBlock.setRootPostBlockVersionId(currentPostBlock.getPred().getRootPostBlockVersionId());
                    }
                }
            }

            if (succIndex==this.size()) {
                // current is last element
                currentVersion.setSuccPostHistoryId(null);
            } else {
                currentVersion.setSuccPostHistoryId(this.get(succIndex).getPostHistoryId());
            }
        }

        // calculate diffs
        diffs.fromPostVersionList(this);
    }

    public PostBlockDiffList getDiffs() {
        return diffs;
    }

    public void insert(StatelessSession session) {
        for (PostVersion currentVersion : this) {
            // save current post version
            session.insert(currentVersion);
            // update the post blocks (pred, succ, similarity, root post block)
            currentVersion.updateBlocks(session);
        }

        diffs.insert(session);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        for (PostVersion version : this) {
            str.append(version.toString());
            str.append("\n");
        }

        return str.toString();
    }

}
