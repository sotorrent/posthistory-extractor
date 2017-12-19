package de.unitrier.st.soposthistory.blocks;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.Config;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Set;

@Entity
@DiscriminatorValue(value="1")
public class TextBlockVersion extends PostBlockVersion {
    public static final int postBlockTypeId = 1;

    public TextBlockVersion() {
        super();
    }

    public TextBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    public static Set<Integer> getPostBlockTypeIdFilter() {
        return Sets.newHashSet(TextBlockVersion.postBlockTypeId);
    }

    @Override
    public int getPostBlockTypeId() {
        return TextBlockVersion.postBlockTypeId;
    }

    @Override
    public boolean isSelected(Set<Integer> postBlockTypeFilter) {
        return postBlockTypeFilter.contains(TextBlockVersion.postBlockTypeId);
    }

    @Override
    public PostBlockSimilarity compareTo(PostBlockVersion otherBlock, Config config) {
        return compareTo(otherBlock, config.getTextSimilarityMetric(), config.getTextBackupSimilarityMetric());
    }

    @Override
    void retrieveMatchingPredecessors(Config config) {
        // retrieve predecessors with maximal similarity

        // return if maximum similarity is below the configured similarity thresholds
        boolean similarityBelowThreshold = maxSimilarity < config.getTextSimilarityThreshold();
        boolean backupSimilarityBelowThreshold = maxBackupSimilarity < config.getTextBackupSimilarityThreshold();
        boolean backupMetricConfigured = config.getTextBackupSimilarityMetric() != null;

        if ((!backupMetricConfigured && similarityBelowThreshold)
                || (backupMetricConfigured && similarityBelowThreshold && backupSimilarityBelowThreshold)) {
            return;
        }

        // retrieve matching predecessors
        retrieveMatchingPredecessors(config.getTextSimilarityThreshold(), config.getTextBackupSimilarityThreshold());
    }

    @Override
    public String toString() {
        return "TextBlockVersion: " + getContent();
    }
}
