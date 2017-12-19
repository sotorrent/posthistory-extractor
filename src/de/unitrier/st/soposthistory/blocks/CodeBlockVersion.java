package de.unitrier.st.soposthistory.blocks;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.Config;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Set;

@Entity
@DiscriminatorValue(value="2")
public class CodeBlockVersion extends PostBlockVersion {
    public static final int postBlockTypeId = 2;

    // TODO: Derive programming language from question tags and/or content of code block. Other option: HTML comments (see http://stackoverflow.com/editing-help#syntax-highlighting)

    public CodeBlockVersion() {
        super();
    }

    public CodeBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    public static Set<Integer> getPostBlockTypeIdFilter() {
        return Sets.newHashSet(CodeBlockVersion.postBlockTypeId);
    }

    @Override
    public int getPostBlockTypeId() {
        return CodeBlockVersion.postBlockTypeId;
    }

    @Override
    public boolean isSelected(Set<Integer> postBlockTypeFilter) {
        return postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId);
    }

    @Override
    public PostBlockSimilarity compareTo(PostBlockVersion otherBlock, Config config) {
        return compareTo(otherBlock, config.getCodeSimilarityMetric(), config.getCodeBackupSimilarityMetric());
    }

    @Override
    void retrieveMatchingPredecessors(Config config) {
        // retrieve predecessors with maximal similarity

        // return if maximum similarity is below the configured similarity thresholds
        boolean similarityBelowThreshold = maxSimilarity < config.getCodeSimilarityThreshold();
        boolean backupSimilarityBelowThreshold = maxBackupSimilarity < config.getCodeBackupSimilarityThreshold();
        boolean backupMetricConfigured = config.getCodeBackupSimilarityMetric() != null;

        if ((!backupMetricConfigured && similarityBelowThreshold)
                || (backupMetricConfigured && similarityBelowThreshold && backupSimilarityBelowThreshold)) {
            return;
        }

        // retrieve matching predecessors
        retrieveMatchingPredecessors(config.getCodeSimilarityThreshold(), config.getCodeBackupSimilarityThreshold());
    }

    @Override
    public String toString() {
        return "CodeBlockVersion: " + getContent();
    }
}
