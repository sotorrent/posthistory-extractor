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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

// TODO: move to metrics comparison project

public class MetricComparisonManager {
    private static Logger logger = null;
    private static final CSVFormat csvFormatPostIds;

    private Map<Integer, PostGroundTruth> groundTruth;
    private Map<Integer, PostVersionList> postHistory;
    private List<BiFunction<String, String, Double>> similarityMetrics;
    private List<Double> similarityThresholds;

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
        similarityMetrics = new LinkedList<>();
        similarityThresholds = new LinkedList<>();
        addSimilarityMetrics();
        addSimilarityThresholds();
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

    private void addSimilarityThresholds() {
        for (double threshold=0.3; threshold<0.99; threshold+=0.1) {
            similarityThresholds.add(threshold);
        }
    }

    private void addSimilarityMetrics() {
        // ****** Edit based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshtein);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized);

        // ****** Fingerprint based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized);

        // ****** Profile based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized);

        // ****** Set based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized);

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramSimilarityKondrak05);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramSimilarityKondrak05);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramSimilarityKondrak05);
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramSimilarityKondrak05);
    }
}
