package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.stringsimilarity.util.InputTooShortException;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

// TODO: move to metrics comparison project
public class MetricComparison {
    final private int postId;
    final private List<Integer> postHistoryIds;
    final private PostVersionList postVersionList;
    final private PostGroundTruth postGroundTruth;
    final private BiFunction<String, String, Double> similarityMetric;
    final private String similarityMetricName;
    final private double similarityThreshold;
    final private StopWatch stopWatch;
    private boolean inputTooShort;
    private int repetitionCount;
    private int currentRepetition;

    // text
    private double runtimeText;
    // PostHistoryId -> metric results for text blocks
    private Map<Integer, MetricResult> resultsText;

    // code
    private double runtimeCode;
    // PostHistoryId -> metric results for code blocks
    private Map<Integer, MetricResult> resultsCode;

    public MetricComparison(int postId,
                            PostVersionList postVersionList,
                            PostGroundTruth postGroundTruth,
                            BiFunction<String, String, Double> similarityMetric,
                            String similarityMetricName,
                            double similarityThreshold,
                            int repetitionCount) {
        this.postId = postId;
        this.postVersionList = postVersionList;
        // normalize links so that post version list and ground truth are comparable
        postVersionList.normalizeLinks();
        this.postGroundTruth = postGroundTruth;
        this.postHistoryIds = postVersionList.getPostHistoryIds();

        if (!this.postGroundTruth.getPostHistoryIds().equals(this.postHistoryIds)) {
            throw new IllegalArgumentException("PostHistoryIds in postVersionList and postGroundTruth differ.");
        }

        this.similarityMetric = similarityMetric;
        this.similarityMetricName = similarityMetricName;
        this.similarityThreshold = similarityThreshold;
        this.inputTooShort = false;
        this.resultsText = new HashMap<>();
        this.resultsCode = new HashMap<>();

        this.repetitionCount = repetitionCount;
        this.currentRepetition = 0;
        stopWatch = new StopWatch();
    }

    public void start() {
        Config config = Config.METRICS_COMPARISON
                .withTextSimilarityMetric(similarityMetric)
                .withTextSimilarityThreshold(similarityThreshold)
                .withCodeSimilarityMetric(similarityMetric)
                .withCodeSimilarityThreshold(similarityThreshold);

        // the post version list is shared by all metric comparisons conducted for the corresponding post
        synchronized (postVersionList) {
            currentRepetition++;

            // process version history of text blocks
            stopWatch.reset();
            stopWatch.start();
            try {
                postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            }
            stopWatch.stop();
            setResultsText();

            // reset post block version history
            postVersionList.resetPostBlockVersionHistory();

            // process version history of code blocks
            stopWatch.reset();
            stopWatch.start();
            try {
                postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            }
            stopWatch.stop();
            setResultsCode();
        }
    }

    private void setResultsText() {
        runtimeText = setResults(resultsText, runtimeText, TextBlockVersion.getPostBlockTypeIdFilter());
    }

    private void setResultsCode() {
        runtimeCode = setResults(resultsCode, runtimeCode, CodeBlockVersion.getPostBlockTypeIdFilter());
    }

    private double setResults(Map<Integer, MetricResult> results, double runtime, Set<Integer> postBlockTypeFilter) {
        if (currentRepetition == 1) {
            // set initial values after first run, return runtime
            for (int postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, postBlockTypeFilter);
                results.put(postHistoryId, result);
            }
            return runtime;
        } else {
            // compare result values in later runs
            for (int postHistoryId : postHistoryIds) {
                MetricResult resultInMap = results.get(postHistoryId);
                MetricResult newResult = getResults(postHistoryId, postBlockTypeFilter);
                if (!resultInMap.truePositives.equals(newResult.truePositives) ||
                    !resultInMap.falsePositives.equals(newResult.falsePositives) ||
                    !resultInMap.trueNegatives.equals(newResult.trueNegatives) ||
                    !resultInMap.falseNegatives.equals(newResult.falseNegatives)) {

                    throw new IllegalStateException("Metric results changed from repetition "
                            + (currentRepetition-1) + " to " + currentRepetition);
                }
            }

            if (currentRepetition < repetitionCount) {
                // return sum of runtimes
                return runtime + stopWatch.getTime();
            }
            else {
                // calculate and return mean runtime after last run
                return (runtime + stopWatch.getTime()) / (double) repetitionCount;
            }
        }
    }

    private MetricResult getResults(int postHistoryId, Set<Integer> postBlockTypeFilter) {
        MetricResult result = new MetricResult();

        if (!inputTooShort) {
            int possibleConnections = postGroundTruth.getPossibleConnections(postHistoryId, postBlockTypeFilter);
            Set<PostBlockConnection> postBlockConnections = postVersionList.getPostVersion(postHistoryId).getConnections(postBlockTypeFilter);
            Set<PostBlockConnection> postBlockConnectionsGT = postGroundTruth.getConnections(postHistoryId, postBlockTypeFilter);

            int truePositivesCount = PostBlockConnection.intersection(postBlockConnectionsGT, postBlockConnections).size();
            int falsePositivesCount = PostBlockConnection.difference(postBlockConnections, postBlockConnectionsGT).size();

            int trueNegativesCount = possibleConnections - (PostBlockConnection.union(postBlockConnectionsGT, postBlockConnections).size());
            int falseNegativesCount = PostBlockConnection.difference(postBlockConnectionsGT, postBlockConnections).size();

            int allConnectionsCount = truePositivesCount + falsePositivesCount + trueNegativesCount + falseNegativesCount;
            if (possibleConnections != allConnectionsCount) {
                throw new IllegalStateException("Invalid result (expected: " + possibleConnections
                        + "; actual: " + allConnectionsCount + ")");
            }

            result.truePositives = truePositivesCount;
            result.falsePositives = falsePositivesCount;
            result.trueNegatives = trueNegativesCount;
            result.falseNegatives = falseNegativesCount;
        }

        return result;
    }

    public BiFunction<String, String, Double> getSimilarityMetric() {
        return similarityMetric;
    }

    public String getSimilarityMetricName() {
        return similarityMetricName;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getPostId() {
        return postId;
    }

    public double getRuntimeText() {
        return runtimeText;
    }

    public MetricResult getResultsText(int postHistoryId) {
        return resultsText.get(postHistoryId);
    }

    public double getRuntimeCode() {
        return runtimeCode;
    }

    public MetricResult getResultsCode(int postHistoryId) {
        return resultsCode.get(postHistoryId);
    }

    public class MetricResult {
        // after last repetition, variables hold mean values
        Integer truePositives = null;
        Integer falsePositives = null;
        Integer trueNegatives = null;
        Integer falseNegatives = null;

        public Integer getTruePositives() {
            return truePositives;
        }

        public Integer getFalsePositives() {
            return falsePositives;
        }

        public Integer getTrueNegatives() {
            return trueNegatives;
        }

        public Integer getFalseNegatives() {
            return falseNegatives;
        }
    }
}
