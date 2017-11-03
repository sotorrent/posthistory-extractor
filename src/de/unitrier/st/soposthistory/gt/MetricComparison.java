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
            getResultsText();

            postVersionList.resetPostBlockVersionHistory();

            stopWatch.reset();
            stopWatch.start();
            try {
                postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
            } catch (InputTooShortException e) {
                inputTooShort = true;
            }
            stopWatch.stop();
            getResultsCode();
        }
    }

    private void getResultsText() {
        runtimeText = stopWatch.getTime();
        for (Integer postHistoryId : postHistoryIds) {
            // text
            Integer truePositivesTextCount = null;
            Integer falsePositivesTextCount = null;
            Integer trueNegativesTextCount = null;
            Integer falseNegativesTextCount = null;

            if (!inputTooShort) {
                int possibleConnectionsText = postGroundTruth.getPossibleConnections(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnectionsText = postVersionList.getPostVersion(postHistoryId).getConnections(TextBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnectionsTextGT = postGroundTruth.getConnections(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());

                truePositivesTextCount = PostBlockConnection.intersection(postBlockConnectionsTextGT, postBlockConnectionsText).size();
                falsePositivesTextCount = PostBlockConnection.difference(postBlockConnectionsText, postBlockConnectionsTextGT).size();

                trueNegativesTextCount = possibleConnectionsText - (PostBlockConnection.union(postBlockConnectionsTextGT, postBlockConnectionsText).size());
                falseNegativesTextCount = PostBlockConnection.difference(postBlockConnectionsTextGT, postBlockConnectionsText).size();
            }

            truePositivesText.put(postHistoryId, truePositivesTextCount);
            falsePositivesText.put(postHistoryId, falsePositivesTextCount);
            trueNegativesText.put(postHistoryId, trueNegativesTextCount);
            falseNegativesText.put(postHistoryId, falseNegativesTextCount);
        }
    }

    private void getResultsCode() {
        runtimeCode = stopWatch.getTime();
        for (Integer postHistoryId : postHistoryIds) {
            Integer truePositivesCodeCount = null;
            Integer falsePositivesCodeCount = null;
            Integer trueNegativesCodeCount = null;
            Integer falseNegativesCodeCount = null;

            if (!inputTooShort) {
                int possibleConnectionsCode = postGroundTruth.getPossibleConnections(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnectionsCode = postVersionList.getPostVersion(postHistoryId).getConnections(CodeBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnectionsCodeGT = postGroundTruth.getConnections(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());

                truePositivesCodeCount = PostBlockConnection.intersection(postBlockConnectionsCodeGT, postBlockConnectionsCode).size();
                falsePositivesCodeCount = PostBlockConnection.difference(postBlockConnectionsCode, postBlockConnectionsCodeGT).size();

                trueNegativesCodeCount = possibleConnectionsCode - (PostBlockConnection.union(postBlockConnectionsCodeGT, postBlockConnectionsCode).size());
                falseNegativesCodeCount = PostBlockConnection.difference(postBlockConnectionsCodeGT, postBlockConnectionsCode).size();
            }

            truePositivesCode.put(postHistoryId, truePositivesCodeCount);
            falsePositivesCode.put(postHistoryId, falsePositivesCodeCount);
            trueNegativesCode.put(postHistoryId, trueNegativesCodeCount);
            falseNegativesCode.put(postHistoryId, falseNegativesCodeCount);
        }
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
}
