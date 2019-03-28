package org.sotorrent.posthistoryextractor.blocks;

import com.google.common.collect.Sets;
import org.sotorrent.posthistoryextractor.Config;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Set;

@Entity
@DiscriminatorValue(value="2")
public class CodeBlockVersion extends PostBlockVersion {
    public static final byte postBlockTypeId = 2;

    public CodeBlockVersion() {
        super();
    }

    public CodeBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    public static Set<Byte> getPostBlockTypeIdFilter() {
        return Sets.newHashSet(CodeBlockVersion.postBlockTypeId);
    }

    @Override
    @Transient
    public Byte getPostBlockTypeId() {
        return CodeBlockVersion.postBlockTypeId;
    }

    @Override
    @Transient
    public boolean isSelected(Set<Byte> postBlockTypeFilter) {
        return postBlockTypeFilter.contains(CodeBlockVersion.postBlockTypeId);
    }

    @Override
    @Transient
    public PostBlockSimilarity compareTo(PostBlockVersion otherBlock, Config config) {
        return compareTo(otherBlock, config.getCodeSimilarityMetric(), config.getCodeBackupSimilarityMetric(), config.getEditSimilarityMetric());
    }

    @Override
    void retrieveMatchingPredecessors(Config config) {
        // retrieve predecessors with maximal similarity

        // return if maximum similarity is below the configured similarity thresholds
        boolean maxSimilarityBelowThreshold;
        if (maxSimilarity.isBackupSimilarity()) {
            maxSimilarityBelowThreshold = maxSimilarity.getMetricResult() < config.getCodeBackupSimilarityThreshold();
        } else {
            maxSimilarityBelowThreshold = maxSimilarity.getMetricResult() < config.getCodeSimilarityThreshold();
        }
        if (maxSimilarityBelowThreshold) {
            return;
        }

        // retrieve matching predecessors
        retrieveMatchingPredecessors(config.getCodeSimilarityThreshold(), config.getCodeBackupSimilarityThreshold(), config.getEditSimilarityThreshold());
    }

    @Override
    public String toString() {
        return "CodeBlockVersion: " + getContent();
    }
}
