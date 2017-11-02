package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.util.Config;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.apache.commons.lang3.time.StopWatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

// TODO: move to metrics comparison project
public class MetricComparison {
    private int postId;
    private List<Integer> postHistoryIds;
    private PostVersionList postVersionList;
    private PostGroundTruth postGroundTruth;
    private BiFunction<String, String, Double> similarityMetric;
    private String similarityMetricName;
    private double similarityThreshold;
    private StopWatch stopWatch;

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

        stopWatch.reset();
        stopWatch.start();
        try {
            postVersionList.processVersionHistory(config, TextBlockVersion.getPostBlockTypeIdFilter());
        }catch(NullPointerException e){}
        stopWatch.stop();
        runtimeText = stopWatch.getTime();

        postVersionList.reset();

        stopWatch.reset();
        stopWatch.start();
        try{
            postVersionList.processVersionHistory(config, CodeBlockVersion.getPostBlockTypeIdFilter());
        }catch(NullPointerException e){}
        stopWatch.stop();
        runtimeCode = stopWatch.getTime();

        getResults();
    }

    private void getResults() {
        // TODO: Lorik
        for(Integer postHistoryId : postHistoryIds){
            // text
            try {
                Set<PostBlockConnection> postBlockConnections_text = postVersionList.getPostVersion(postHistoryId).getConnections(TextBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnections_text_gt = postGroundTruth.getConnections(postHistoryId, TextBlockVersion.getPostBlockTypeIdFilter());

                falseNegativesText.put(postHistoryId, Math.abs(PostBlockConnection.difference(postBlockConnections_text_gt, postBlockConnections_text).size()));
                truePositivesText.put(postHistoryId, Math.abs(PostBlockConnection.intersection(postBlockConnections_text_gt, postBlockConnections_text).size()));
                falsePositivesText.put(postHistoryId, Math.abs(PostBlockConnection.difference(postBlockConnections_text, postBlockConnections_text_gt).size()));
                trueNegativesText.put(postHistoryId, Math.abs(postGroundTruth.getPossibleConnections(postHistoryId) - (PostBlockConnection.union(postBlockConnections_text_gt, postBlockConnections_text).size())));
            }catch(NullPointerException e){
                falseNegativesText.put(postHistoryId, null);
                truePositivesText.put(postHistoryId, null);
                falsePositivesText.put(postHistoryId, null);
                trueNegativesText.put(postHistoryId, null);
            }

            // code
            try {
                Set<PostBlockConnection> postBlockConnections_code = postVersionList.getPostVersion(postHistoryId).getConnections(CodeBlockVersion.getPostBlockTypeIdFilter());
                Set<PostBlockConnection> postBlockConnections_code_gt = postGroundTruth.getConnections(postHistoryId, CodeBlockVersion.getPostBlockTypeIdFilter());

                falseNegativesCode.put(postHistoryId, PostBlockConnection.difference(postBlockConnections_code_gt, postBlockConnections_code).size());
                truePositivesCode.put(postHistoryId, PostBlockConnection.intersection(postBlockConnections_code_gt, postBlockConnections_code).size());
                falsePositivesCode.put(postHistoryId, PostBlockConnection.difference(postBlockConnections_code, postBlockConnections_code_gt).size());
                trueNegativesCode.put(postHistoryId, postGroundTruth.getPossibleConnections(postHistoryId) - (PostBlockConnection.union(postBlockConnections_code_gt, postBlockConnections_code).size()));
            }catch(NullPointerException e){
                falseNegativesCode.put(postHistoryId, null);
                truePositivesCode.put(postHistoryId, null);
                falsePositivesCode.put(postHistoryId, null);
                trueNegativesCode.put(postHistoryId, null);
            }
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
