package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.function.BiFunction;

// TODO: move to metrics comparison project
public class MetricComparison {
    private int postId;
    private List<Integer> postHistoryIds;
    private PostVersionList postVersionList;
    private PostGroundTruth postGroundTruth;
    private BiFunction<String, String, Double> similarityMetric;
    private double similarityThreshold;
    StopWatch stopWatch;

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
                            double similarityThreshold) {
        this.postId = postId;
        this.postHistoryIds = postVersionList.getPostHistoryIds();
        this.postVersionList = postVersionList;
        this.postGroundTruth = postGroundTruth;
        this.similarityMetric = similarityMetric;
        this.similarityThreshold = similarityThreshold;
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
    }

    public void start() {
        Config config = Config.METRICS_COMPARISON
                .withTextSimilarityMetric(similarityMetric)
                .withTextSimilarityThreshold(similarityThreshold)
                .withCodeSimilarityMetric(similarityMetric)
                .withCodeSimilarityThreshold(similarityThreshold);

        stopWatch.reset();
        stopWatch.start();
        postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
        stopWatch.stop();
        runtimeText = stopWatch.getTime();

        postVersionList.reset();

        stopWatch.reset();
        stopWatch.start();
        postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
        stopWatch.stop();
        runtimeCode = stopWatch.getTime();

        getResults();
    }

    private void getResults() {
        // TODO: Lorik
    }

    public BiFunction<String, String, Double> getSimilarityMetric() {
        return similarityMetric;
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
