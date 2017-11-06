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
    // PostHistoryId -> mean number of #false/true positives/negatives
    private Map<Integer, Double> truePositivesText;
    private Map<Integer, Double> falsePositivesText;
    private Map<Integer, Double> trueNegativesText;
    private Map<Integer, Double> falseNegativesText;

    // code
    private double runtimeCode;
    // PostHistoryId -> mean number of #false/true positives/negatives
    private Map<Integer, Double> truePositivesCode;
    private Map<Integer, Double> falsePositivesCode;
    private Map<Integer, Double> trueNegativesCode;
    private Map<Integer, Double> falseNegativesCode;

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
        // text
        this.truePositivesText = new HashMap<>();
        this.falsePositivesText = new HashMap<>();
        this.trueNegativesText = new HashMap<>();
        this.falseNegativesText = new HashMap<>();
        // code
        this.truePositivesCode = new HashMap<>();
        this.falsePositivesCode = new HashMap<>();
        this.trueNegativesCode = new HashMap<>();
        this.falseNegativesCode = new HashMap<>();

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
        if (currentRepetition == 1) {
            // set initial values after first run
            runtimeText = stopWatch.getTime();
            for (Integer postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());
                truePositivesText.put(postHistoryId, result.truePositives);
                falsePositivesText.put(postHistoryId, result.falsePositives);
                trueNegativesText.put(postHistoryId, result.trueNegatives);
                falseNegativesText.put(postHistoryId, result.falseNegatives);
            }
        } else {
            // sum up values in later runs
            runtimeText = runtimeText + stopWatch.getTime();
            for (Integer postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());
                truePositivesText.put(postHistoryId, truePositivesText.get(postHistoryId) + result.truePositives);
                falsePositivesText.put(postHistoryId, falsePositivesText.get(postHistoryId) + result.falsePositives);
                trueNegativesText.put(postHistoryId, trueNegativesText.get(postHistoryId) + result.trueNegatives);
                falseNegativesText.put(postHistoryId, falseNegativesText.get(postHistoryId) + result.falseNegatives);
            }
        }

        // calculate mean after last run
        if (currentRepetition == repetitionCount) {
            runtimeText = runtimeText / (double)repetitionCount;
            for (Integer postHistoryId : postHistoryIds) {
                truePositivesText.put(postHistoryId, truePositivesText.get(postHistoryId) / (double)repetitionCount);
                falsePositivesText.put(postHistoryId, falsePositivesText.get(postHistoryId)  / (double)repetitionCount);
                trueNegativesText.put(postHistoryId, trueNegativesText.get(postHistoryId) / (double)repetitionCount);
                falseNegativesText.put(postHistoryId, falseNegativesText.get(postHistoryId)  / (double)repetitionCount);
            }
        }
    }

    private void setResultsCode() {
        if (currentRepetition == 1) {
            // set initial values after first run
            runtimeCode = stopWatch.getTime();
            for (Integer postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());
                truePositivesCode.put(postHistoryId, result.truePositives);
                falsePositivesCode.put(postHistoryId, result.falsePositives);
                trueNegativesCode.put(postHistoryId, result.trueNegatives);
                falseNegativesCode.put(postHistoryId, result.falseNegatives);
            }
        } else {
            // sum up values in later runs
            runtimeCode = runtimeCode + stopWatch.getTime();
            for (Integer postHistoryId : postHistoryIds) {
                MetricResult result = getResults(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());
                truePositivesCode.put(postHistoryId, truePositivesCode.get(postHistoryId) + result.truePositives);
                falsePositivesCode.put(postHistoryId, falsePositivesCode.get(postHistoryId) + result.falsePositives);
                trueNegativesCode.put(postHistoryId, trueNegativesCode.get(postHistoryId) + result.trueNegatives);
                falseNegativesCode.put(postHistoryId, falseNegativesCode.get(postHistoryId) + result.falseNegatives);
            }
        }

        // calculate mean after last run
        if (currentRepetition == repetitionCount) {
            runtimeCode = runtimeCode / (double)repetitionCount;
            for (Integer postHistoryId : postHistoryIds) {
                truePositivesCode.put(postHistoryId, truePositivesCode.get(postHistoryId) / (double)repetitionCount);
                falsePositivesCode.put(postHistoryId, falsePositivesCode.get(postHistoryId)  / (double)repetitionCount);
                trueNegativesCode.put(postHistoryId, trueNegativesCode.get(postHistoryId) / (double)repetitionCount);
                falseNegativesCode.put(postHistoryId, falseNegativesCode.get(postHistoryId)  / (double)repetitionCount);
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

            result.truePositives = (double)truePositivesCount;
            result.falsePositives = (double)falsePositivesCount;
            result.trueNegatives = (double)trueNegativesCount;
            result.falseNegatives = (double)falseNegativesCount;
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

    public Map<Integer, Double> getTruePositivesText() {
        return truePositivesText;
    }

    public Map<Integer, Double> getFalsePositivesText() {
        return falsePositivesText;
    }

    public Map<Integer, Double> getTrueNegativesText() {
        return trueNegativesText;
    }

    public Map<Integer, Double> getFalseNegativesText() {
        return falseNegativesText;
    }

    public double getRuntimeCode() {
        return runtimeCode;
    }

    public Map<Integer, Double> getTruePositivesCode() {
        return truePositivesCode;
    }

    public Map<Integer, Double> getFalsePositivesCode() {
        return falsePositivesCode;
    }

    public Map<Integer, Double> getTrueNegativesCode() {
        return trueNegativesCode;
    }

    public Map<Integer, Double> getFalseNegativesCode() {
        return falseNegativesCode;
    }

    private class MetricResult {
        Double truePositives = null;
        Double falsePositives = null;
        Double trueNegatives = null;
        Double falseNegatives = null;
    }
}
