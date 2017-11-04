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

    // text
    private long runtimeText;
    private Map<Integer, Integer> truePositivesText;
    private Map<Integer, Integer> falsePositivesText; // TODO: PostHistoryId -> #false positives
    private Map<Integer, Integer> trueNegativesText;
    private Map<Integer, Integer> falseNegativesText;

    // code
    private long runtimeCode;
    private Map<Integer, Integer> truePositivesCode;
    private Map<Integer, Integer> falsePositivesCode;
    private Map<Integer, Integer> trueNegativesCode;
    private Map<Integer, Integer> falseNegativesCode;

    public MetricComparison(int postId,
                            PostVersionList postVersionList,
                            PostGroundTruth postGroundTruth,
                            BiFunction<String, String, Double> similarityMetric,
                            String similarityMetricName,
                            double similarityThreshold) {
        this.postId = postId;
        this.postVersionList = postVersionList;
        this.postGroundTruth = postGroundTruth;
        this.postHistoryIds = postVersionList.getPostHistoryIds();

        if (!postGroundTruth.getPostHistoryIds().equals(postHistoryIds)) {
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

        stopWatch = new StopWatch();

        postVersionList.normalizeLinks();
    }

    public void start() {
        Config config = Config.METRICS_COMPARISON
                .withTextSimilarityMetric(similarityMetric)
                .withTextSimilarityThreshold(similarityThreshold)
                .withCodeSimilarityMetric(similarityMetric)
                .withCodeSimilarityThreshold(similarityThreshold);

        // the post version list is shared by all metric comparisons conducted for the corresponding post
        synchronized (postVersionList) {
            stopWatch.reset();
            stopWatch.start();
            try {
                postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            }
            stopWatch.stop();
            setResultsText();

            postVersionList.resetPostBlockVersionHistory();

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
        runtimeText = stopWatch.getTime();
        for (Integer postHistoryId : postHistoryIds) {
            MetricResult result = getResults(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());
            truePositivesText.put(postHistoryId, result.truePositives);
            falsePositivesText.put(postHistoryId, result.falsePositives);
            trueNegativesText.put(postHistoryId, result.trueNegatives);
            falseNegativesText.put(postHistoryId, result.falseNegatives);
        }
    }

    private void setResultsCode() {
        runtimeCode = stopWatch.getTime();
        for (Integer postHistoryId : postHistoryIds) {
            MetricResult result = getResults(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());
            truePositivesCode.put(postHistoryId, result.truePositives);
            falsePositivesCode.put(postHistoryId, result.falsePositives);
            trueNegativesCode.put(postHistoryId, result.trueNegatives);
            falseNegativesCode.put(postHistoryId, result.falseNegatives);
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

    public long getRuntimeText() {
        return runtimeText;
    }

    public Map<Integer, Integer> getTruePositivesText() {
        return truePositivesText;
    }

    public Map<Integer, Integer> getFalsePositivesText() {
        return falsePositivesText;
    }

    public Map<Integer, Integer> getTrueNegativesText() {
        return trueNegativesText;
    }

    public Map<Integer, Integer> getFalseNegativesText() {
        return falseNegativesText;
    }

    public long getRuntimeCode() {
        return runtimeCode;
    }

    public Map<Integer, Integer> getTruePositivesCode() {
        return truePositivesCode;
    }

    public Map<Integer, Integer> getFalsePositivesCode() {
        return falsePositivesCode;
    }

    public Map<Integer, Integer> getTrueNegativesCode() {
        return trueNegativesCode;
    }

    public Map<Integer, Integer> getFalseNegativesCode() {
        return falseNegativesCode;
    }

    private class MetricResult {
        public Integer truePositives = null;
        public Integer falsePositives = null;
        public Integer trueNegatives = null;
        public Integer falseNegatives = null;
    }
}
