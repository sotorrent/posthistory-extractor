package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.stringsimilarity.util.IllegalSimilarityValueException;
import de.unitrier.st.stringsimilarity.util.InputTooShortException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.List;
import java.util.function.BiFunction;

@Entity
@DiscriminatorValue(value="1")
public class TextBlockVersion extends PostBlockVersion {
    // TODO: handling of URL blocks (tranform to inline URLs, see AnchorTextAndUrlHandler)

    public static final int postBlockTypeId = 1;
    public static BiFunction<String, String, Double> similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap; //TODO: update this after evaluation
    public static BiFunction<String, String, Double> backupSimilarityMetric = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein; // TODO: use best edit-based metric here
    public static double similarityThreshold = 0.6; //TODO: update this after evaluation (two thresholds needed because of backup)

    public TextBlockVersion() {
        super();
    }

    public TextBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    @Override
    public double compareTo(PostBlockVersion otherBlock) {
        double similarity;
        try {
            similarity = similarityMetric.apply(getContent(), otherBlock.getContent());
        } catch (InputTooShortException e) {
            similarity = backupSimilarityMetric.apply(getContent(), otherBlock.getContent());
        }

        if (similarity < 0.0 || similarity > 1.0) {
            throw new IllegalSimilarityValueException("Similarity value must be in range [0.0, 1.0], but was " + similarity);
        }

        return similarity;
    }

    @Override
    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks) {
        return super.findMatchingPredecessors(previousVersionPostBlocks, similarityThreshold);
    }

    @Override
    public String toString() {
        return "TextBlockVersion: " + getContent();
    }
}
