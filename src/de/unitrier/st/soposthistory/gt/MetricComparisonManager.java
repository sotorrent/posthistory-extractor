package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.csv.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static de.unitrier.st.soposthistory.util.Util.getClassLogger;

// TODO: move to metrics comparison project
public class MetricComparisonManager {
    private static Logger logger = null;
    private static final CSVFormat csvFormatPostIds;
    public static final CSVFormat csvFormatMetricComparison;

    private String name;
    private int repetitionCount;
    private boolean randomizeOrder;
    private Set<Integer> postIds;
    private Map<Integer, List<Integer>> postHistoryIds;
    private Map<Integer, PostGroundTruth> postGroundTruth;
    private Map<Integer, PostVersionList> postVersionLists;
    private List<BiFunction<String, String, Double>> similarityMetrics;
    private List<String> similarityMetricsNames;
    private List<Double> similarityThresholds;

    private List<MetricComparison> metricComparisons;

    static {
        // configure logger
        try {
            logger = getClassLogger(MetricComparisonManager.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // configure CSV format for list of PostIds
        csvFormatPostIds = CSVFormat.DEFAULT
                .withHeader("PostId", "PostTypeId", "VersionCount")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\');

        // configure CSV format for metric comparison results
        csvFormatMetricComparison = CSVFormat.DEFAULT
                .withHeader("Sample", "Metric", "Threshold", "PostId", "PostHistoryId", "RuntimeText", "TruePositivesText", "TrueNegativesText", "FalsePositivesText", "FalseNegativesText", "RuntimeCode", "TruePositivesCode", "TrueNegativesCode", "FalsePositivesCode", "FalseNegativesCode")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withNullString("null");
    }

    private MetricComparisonManager(String name, boolean addDefaultMetricsAndThresholds,
                                    int repetitionCount, boolean randomizeOrder) {
        this.name = name;
        this.repetitionCount = repetitionCount;
        this.randomizeOrder = randomizeOrder;
        this.postIds = new HashSet<>();
        this.postHistoryIds = new HashMap<>();
        this.postGroundTruth = new HashMap<>();
        this.postVersionLists = new HashMap<>();
        this.similarityMetrics = new LinkedList<>();
        this.similarityMetricsNames = new LinkedList<>();
        this.similarityThresholds = new LinkedList<>();
        this.metricComparisons = new LinkedList<>();
        if (addDefaultMetricsAndThresholds) {
            addDefaultSimilarityMetrics();
            addDefaultSimilarityThresholds();
        }
    }

    public static MetricComparisonManager create(String name,
                                                 Path postIdPath,
                                                 Path postHistoryPath,
                                                 Path groundTruthPath) {
        return create(name, postIdPath, postHistoryPath, groundTruthPath,
                true,
                5,
                true);
    }

    public static MetricComparisonManager create(String name,
                                                 Path postIdPath,
                                                 Path postHistoryPath,
                                                 Path groundTruthPath,
                                                 boolean addDefaultMetricsAndThresholds) {
        return create(name, postIdPath, postHistoryPath, groundTruthPath,
                addDefaultMetricsAndThresholds,
                5,
                true);
    }

    public static MetricComparisonManager create(String name,
                                                 Path postIdPath,
                                                 Path postHistoryPath,
                                                 Path groundTruthPath,
                                                 boolean addDefaultMetricsAndThresholds,
                                                 int repetitionCount,
                                                 boolean randomizeOrder) {
        // ensure that input file exists (directories are tested in read methods)
        if (!Files.exists(postIdPath) || Files.isDirectory(postIdPath)) {
            throw new IllegalArgumentException("File not found: " + postIdPath);
        }

        MetricComparisonManager manager = new MetricComparisonManager(name, addDefaultMetricsAndThresholds,
                repetitionCount, randomizeOrder);

        try (CSVParser csvParser = new CSVParser(new FileReader(postIdPath.toFile()),
                csvFormatPostIds.withFirstRecordAsHeader())) {
            logger.info("Reading PostIds from CSV file " + postIdPath.toFile().toString() + " ...");

            for (CSVRecord currentRecord : csvParser) {
                int postId = Integer.parseInt(currentRecord.get("PostId"));
                int postTypeId = Integer.parseInt(currentRecord.get("PostTypeId"));
                int versionCount = Integer.parseInt(currentRecord.get("VersionCount"));

                // add post id to set
                manager.postIds.add(postId);

                // read post version list
                PostVersionList postVersionList = PostVersionList.readFromCSV(
                        postHistoryPath, postId, postTypeId, false
                );

                if (postVersionList.size() != versionCount) {
                    throw new IllegalArgumentException("Version count expected to be " + versionCount
                            + ", but was " + postVersionList.size()
                    );
                }

                manager.postVersionLists.put(postId, postVersionList);
                manager.postHistoryIds.put(postId, postVersionList.getPostHistoryIds());

                // read ground truth
                PostGroundTruth postGroundTruth = PostGroundTruth.readFromCSV(groundTruthPath, postId);

                if (postGroundTruth.getPossibleConnections() != postVersionList.getPossibleConnections()) {
                    throw new IllegalArgumentException("Number of possible connections in ground truth is different " +
                            "from number of possible connections in post history.");
                }

                manager.postGroundTruth.put(postId, postGroundTruth);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return manager;
    }

    public void compareMetrics() {
        prepareComparison();

        for (int i=1; i<=repetitionCount; i++) {
            if (randomizeOrder) {
                logger.info("Randomizing order...");
                randomizeOrder();
            }

            logger.info("Starting comparison run " + i + "...");
            for (MetricComparison metricComparison : metricComparisons) {
                metricComparison.start();
            }
        }
    }

    private void prepareComparison() {
        for (int postId : postIds) {
            for (double similarityThreshold : similarityThresholds) {
                for (int i = 0; i < similarityMetrics.size(); i++) {
                    BiFunction<String, String, Double> similarityMetric = similarityMetrics.get(i);
                    String similarityMetricName = similarityMetricsNames.get(i);
                    MetricComparison metricComparison = new MetricComparison(
                            postId,
                            postVersionLists.get(postId),
                            postGroundTruth.get(postId),
                            similarityMetric,
                            similarityMetricName,
                            similarityThreshold,
                            repetitionCount
                    );
                    metricComparisons.add(metricComparison);
                }
            }
        }
    }

    private void randomizeOrder() {
        Collections.shuffle(metricComparisons, new Random());
    }

    public void writeToCSV(Path outputDir) {
        // create output directory if it does not exist
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File outputFile = Paths.get(outputDir.toString(), name + ".csv").toFile();

        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                throw new IllegalStateException("Error while deleting output file: " + outputFile);
            }
        }

        // write metric comparison results
        logger.info("Writing metric comparison results to CSV file " + outputFile.getName() + " ...");
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(outputFile), csvFormatMetricComparison)) {
            // header is automatically written
            for (MetricComparison metricComparison : metricComparisons) {
                int postId = metricComparison.getPostId();
                List<Integer> postHistoryIdsForPost = postHistoryIds.get(postId);

                for (int postHistoryId : postHistoryIdsForPost) {
                    csvPrinter.printRecord(
                            name,
                            metricComparison.getSimilarityMetricName(),
                            metricComparison.getSimilarityThreshold(),
                            postId,
                            postHistoryId,
                            metricComparison.getRuntimeText(),
                            metricComparison.getTruePositivesText().get(postHistoryId),
                            metricComparison.getTrueNegativesText().get(postHistoryId),
                            metricComparison.getFalsePositivesText().get(postHistoryId),
                            metricComparison.getFalseNegativesText().get(postHistoryId),
                            metricComparison.getRuntimeCode(),
                            metricComparison.getTruePositivesCode().get(postHistoryId),
                            metricComparison.getTrueNegativesCode().get(postHistoryId),
                            metricComparison.getFalsePositivesCode().get(postHistoryId),
                            metricComparison.getFalseNegativesCode().get(postHistoryId)
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, PostGroundTruth> getPostGroundTruth() {
        return postGroundTruth;
    }

    public Map<Integer, PostVersionList> getPostVersionLists() {
        return postVersionLists;
    }

    public void addSimilarityThreshold(double threshold) {
        similarityThresholds.add(threshold);
    }

    private void addDefaultSimilarityThresholds() {
        similarityThresholds.addAll(Arrays.asList(0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9)); // TODO: add also 0.35, 0.45, 0.55, 0.65, 0.75, 0.85
    }

    public void addSimilarityMetric(String name, BiFunction<String, String, Double> metric) {
        similarityMetricsNames.add(name);
        similarityMetrics.add(metric);
    }

    private void addDefaultSimilarityMetrics() {
        // ****** Edit based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshtein);
        similarityMetricsNames.add("levenshtein");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::levenshteinNormalized);
        similarityMetricsNames.add("levenshteinNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshtein);
        similarityMetricsNames.add("damerauLevenshtein");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::damerauLevenshteinNormalized);
        similarityMetricsNames.add("damerauLevenshteinNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignment);
        similarityMetricsNames.add("optimalAlignment");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::optimalAlignmentNormalized);
        similarityMetricsNames.add("optimalAlignmentNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequence);
        similarityMetricsNames.add("longestCommonSubsequence");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.edit.Variants::longestCommonSubsequenceNormalized);
        similarityMetricsNames.add("longestCommonSubsequenceNormalized");

        // ****** Fingerprint based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccard);
        similarityMetricsNames.add("winnowingTwoGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccard);
        similarityMetricsNames.add("winnowingThreeGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccard);
        similarityMetricsNames.add("winnowingFourGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccard);
        similarityMetricsNames.add("winnowingFiveGramJaccard");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramJaccardNormalized);
        similarityMetricsNames.add("winnowingTwoGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramJaccardNormalized);
        similarityMetricsNames.add("winnowingThreeGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramJaccardNormalized);
        similarityMetricsNames.add("winnowingFourGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramJaccardNormalized);
        similarityMetricsNames.add("winnowingFiveGramJaccardNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDice);
        similarityMetricsNames.add("winnowingTwoGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDice);
        similarityMetricsNames.add("winnowingThreeGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDice);
        similarityMetricsNames.add("winnowingFourGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDice);
        similarityMetricsNames.add("winnowingFiveGramDice");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramDiceNormalized);
        similarityMetricsNames.add("winnowingTwoGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramDiceNormalized);
        similarityMetricsNames.add("winnowingThreeGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized);
        similarityMetricsNames.add("winnowingFourGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramDiceNormalized);
        similarityMetricsNames.add("winnowingFiveGramDiceNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlap);
        similarityMetricsNames.add("winnowingTwoGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlap);
        similarityMetricsNames.add("winnowingThreeGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlap);
        similarityMetricsNames.add("winnowingFourGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlap);
        similarityMetricsNames.add("winnowingFiveGramOverlap");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOverlapNormalized);
        similarityMetricsNames.add("winnowingTwoGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOverlapNormalized);
        similarityMetricsNames.add("winnowingThreeGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOverlapNormalized);
        similarityMetricsNames.add("winnowingFourGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOverlapNormalized);
        similarityMetricsNames.add("winnowingFiveGramOverlapNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingTwoGramLongestCommonSubsequence");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingThreeGramLongestCommonSubsequence");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingFourGramLongestCommonSubsequence");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequence);
        similarityMetricsNames.add("winnowingFiveGramLongestCommonSubsequence");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingTwoGramLongestCommonSubsequenceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingThreeGramLongestCommonSubsequenceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingFourGramLongestCommonSubsequenceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramLongestCommonSubsequenceNormalized);
        similarityMetricsNames.add("winnowingFiveGramLongestCommonSubsequenceNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignment);
        similarityMetricsNames.add("winnowingTwoGramOptimalAlignment");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignment);
        similarityMetricsNames.add("winnowingThreeGramOptimalAlignment");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignment);
        similarityMetricsNames.add("winnowingFourGramOptimalAlignment");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignment);
        similarityMetricsNames.add("winnowingFiveGramOptimalAlignment");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingTwoGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingTwoGramOptimalAlignmentNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingThreeGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingThreeGramOptimalAlignmentNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingFourGramOptimalAlignmentNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFiveGramOptimalAlignmentNormalized);
        similarityMetricsNames.add("winnowingFiveGramOptimalAlignmentNormalized");

        // ****** Profile based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedBool);
        similarityMetricsNames.add("cosineTokenNormalizedBool");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTokenNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTokenNormalizedNormalizedTermFrequency");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedBool);
        similarityMetricsNames.add("cosineTwoGramNormalizedBool");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedBool);
        similarityMetricsNames.add("cosineThreeGramNormalizedBool");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedBool);
        similarityMetricsNames.add("cosineFourGramNormalizedBool");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedBool);
        similarityMetricsNames.add("cosineFiveGramNormalizedBool");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoGramNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeGramNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFourGramNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFiveGramNormalizedTermFrequency");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoGramNormalizedNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeGramNormalizedNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFourGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFourGramNormalizedNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineFiveGramNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineFiveGramNormalizedNormalizedTermFrequency");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedBool);
        similarityMetricsNames.add("cosineTwoShingleNormalizedBool");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedBool);
        similarityMetricsNames.add("cosineThreeShingleNormalizedBool");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoShingleNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeShingleNormalizedTermFrequency");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineTwoShingleNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineTwoShingleNormalizedNormalizedTermFrequency");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::cosineThreeShingleNormalizedNormalizedTermFrequency);
        similarityMetricsNames.add("cosineThreeShingleNormalizedNormalizedTermFrequency");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTokenNormalized);
        similarityMetricsNames.add("manhattanTokenNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoGramNormalized);
        similarityMetricsNames.add("manhattanTwoGramNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeGramNormalized);
        similarityMetricsNames.add("manhattanThreeGramNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized);
        similarityMetricsNames.add("manhattanFourGramNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanFiveGramNormalized);
        similarityMetricsNames.add("manhattanFiveGramNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanTwoShingleNormalized);
        similarityMetricsNames.add("manhattanTwoShingleNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.profile.Variants::manhattanThreeShingleNormalized);
        similarityMetricsNames.add("manhattanThreeShingleNormalized");

        // ****** Set based *****
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccard);
        similarityMetricsNames.add("tokenJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenJaccardNormalized);
        similarityMetricsNames.add("tokenJaccardNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccard);
        similarityMetricsNames.add("twoGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccard);
        similarityMetricsNames.add("threeGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccard);
        similarityMetricsNames.add("fourGramJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccard);
        similarityMetricsNames.add("fiveGramJaccard");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalized);
        similarityMetricsNames.add("twoGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalized);
        similarityMetricsNames.add("threeGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalized);
        similarityMetricsNames.add("fourGramJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalized);
        similarityMetricsNames.add("fiveGramJaccardNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramJaccardNormalizedPadding);
        similarityMetricsNames.add("twoGramJaccardNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramJaccardNormalizedPadding);
        similarityMetricsNames.add("threeGramJaccardNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramJaccardNormalizedPadding);
        similarityMetricsNames.add("fourGramJaccardNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramJaccardNormalizedPadding);
        similarityMetricsNames.add("fiveGramJaccardNormalizedPadding");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccard);
        similarityMetricsNames.add("twoShingleJaccard");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccard);
        similarityMetricsNames.add("threeShingleJaccard");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleJaccardNormalized);
        similarityMetricsNames.add("twoShingleJaccardNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleJaccardNormalized);
        similarityMetricsNames.add("threeShingleJaccardNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDice);
        similarityMetricsNames.add("tokenDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenDiceNormalized);
        similarityMetricsNames.add("tokenDiceNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDice);
        similarityMetricsNames.add("twoGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDice);
        similarityMetricsNames.add("threeGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDice);
        similarityMetricsNames.add("fourGramDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDice);
        similarityMetricsNames.add("fiveGramDice");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalized);
        similarityMetricsNames.add("twoGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalized);
        similarityMetricsNames.add("threeGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalized);
        similarityMetricsNames.add("fourGramDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalized);
        similarityMetricsNames.add("fiveGramDiceNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramDiceNormalizedPadding);
        similarityMetricsNames.add("twoGramDiceNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramDiceNormalizedPadding);
        similarityMetricsNames.add("threeGramDiceNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramDiceNormalizedPadding);
        similarityMetricsNames.add("fourGramDiceNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramDiceNormalizedPadding);
        similarityMetricsNames.add("fiveGramDiceNormalizedPadding");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDice);
        similarityMetricsNames.add("twoShingleDice");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDice);
        similarityMetricsNames.add("threeShingleDice");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleDiceNormalized);
        similarityMetricsNames.add("twoShingleDiceNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleDiceNormalized);
        similarityMetricsNames.add("threeShingleDiceNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlap);
        similarityMetricsNames.add("tokenOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::tokenOverlapNormalized);
        similarityMetricsNames.add("tokenOverlapNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlap);
        similarityMetricsNames.add("twoGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlap);
        similarityMetricsNames.add("threeGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap);
        similarityMetricsNames.add("fourGramOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlap);
        similarityMetricsNames.add("fiveGramOverlap");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalized);
        similarityMetricsNames.add("twoGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalized);
        similarityMetricsNames.add("threeGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalized);
        similarityMetricsNames.add("fourGramOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalized);
        similarityMetricsNames.add("fiveGramOverlapNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramOverlapNormalizedPadding);
        similarityMetricsNames.add("twoGramOverlapNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramOverlapNormalizedPadding);
        similarityMetricsNames.add("threeGramOverlapNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlapNormalizedPadding);
        similarityMetricsNames.add("fourGramOverlapNormalizedPadding");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramOverlapNormalizedPadding);
        similarityMetricsNames.add("fiveGramOverlapNormalizedPadding");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlap);
        similarityMetricsNames.add("twoShingleOverlap");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlap);
        similarityMetricsNames.add("threeShingleOverlap");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoShingleOverlapNormalized);
        similarityMetricsNames.add("twoShingleOverlapNormalized");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeShingleOverlapNormalized);
        similarityMetricsNames.add("threeShingleOverlapNormalized");

        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::twoGramSimilarityKondrak05);
        similarityMetricsNames.add("twoGramSimilarityKondrak05");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::threeGramSimilarityKondrak05);
        similarityMetricsNames.add("threeGramSimilarityKondrak05");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fourGramSimilarityKondrak05);
        similarityMetricsNames.add("fourGramSimilarityKondrak05");
        similarityMetrics.add(de.unitrier.st.stringsimilarity.set.Variants::fiveGramSimilarityKondrak05);
        similarityMetricsNames.add("fiveGramSimilarityKondrak05");
    }

    public MetricComparison getMetricComparison(int postId, String similarityMetricName, double similarityThreshold) {
        for (MetricComparison metricComparison : metricComparisons) {
            if (metricComparison.getPostId() == postId
                    && metricComparison.getSimilarityThreshold() == similarityThreshold
                    && metricComparison.getSimilarityMetricName().equals(similarityMetricName)) {
                return metricComparison;
            }
        }

        return null;
    }
}
