package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.soposthistory.util.Config;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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

    @Override
    public double compareTo(PostBlockVersion otherBlock, Config config) {
        setSimilarityThreshold(config.getTextSimilarityThreshold());
        return compareTo(otherBlock, config.getTextSimilarityMetric(), config.getTextBackupSimilarityMetric());
    }

    @Override
    public String toString() {
        return "TextBlockVersion: " + getContent();
    }
}
