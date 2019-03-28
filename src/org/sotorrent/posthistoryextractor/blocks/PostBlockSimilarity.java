package org.sotorrent.posthistoryextractor.blocks;

public class PostBlockSimilarity {
    private double metricResult;
    private boolean isBackupSimilarity;
    private boolean isEditSimilarity;

    PostBlockSimilarity() {
        this(-1.0, false);
    }

    PostBlockSimilarity(double metricResult) {
        this(metricResult, false);
    }

    PostBlockSimilarity(double metricResult, boolean isBackupSimilarity) {
        this(metricResult, isBackupSimilarity, false);
    }

    PostBlockSimilarity(double metricResult, boolean isBackupSimilarity, boolean isEditSimilarity) {
        this.metricResult = metricResult;
        this.isBackupSimilarity = isBackupSimilarity;
        this.isEditSimilarity = isEditSimilarity;
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

    public boolean isEditSimilarity() {
        return isEditSimilarity;
    }

    public void setEditSimilarity(boolean editSimilarity) {
        isEditSimilarity = editSimilarity;
    }

    @Override
    public String toString() {
        return "(" + metricResult + "; " + isBackupSimilarity + ")";
    }
}
