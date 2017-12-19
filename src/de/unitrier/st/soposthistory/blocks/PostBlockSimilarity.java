package de.unitrier.st.soposthistory.blocks;

public class PostBlockSimilarity {
    private double metricResult;
    private boolean isBackupSimilarity;

    PostBlockSimilarity() {
        this(-1.0, false);
    }

    PostBlockSimilarity(double metricResult) {
        this(metricResult, false);
    }

    PostBlockSimilarity(double metricResult, boolean isBackupSimilarity) {
        this.metricResult = metricResult;
        this.isBackupSimilarity = isBackupSimilarity;
    }

    public double getMetricResult() {
        return metricResult;
    }

    void setMetricResult(double metricResult) {
        this.metricResult = metricResult;
    }

    public boolean isBackupSimilarity() {
        return isBackupSimilarity;
    }

    void setBackupSimilarity(boolean backupSimilarity) {
        isBackupSimilarity = backupSimilarity;
    }

    @Override
    public String toString() {
        return "(" + metricResult + "; " + isBackupSimilarity + ")";
    }
}
