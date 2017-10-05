package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.stringsimilarity.util.InputTooShortException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.function.BiFunction;

@Entity
@DiscriminatorValue(value="1")
public class TextBlockVersion extends PostBlockVersion {
    // TODO: handling of URL blocks (tranform to inline URLs, see AnchorTextAndUrlHandler)

    public static final int blockTypeId = 1;
    public static BiFunction<String, String, Double> similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap; //TODO: update this after evaluation
    public static BiFunction<String, String, Double> backupSimilarityMetric = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein; // TODO: use best text-based metric here
    public static double similarityThreshold = 0.6; //TODO: update this after evaluation (two thresholds needed because of backup)

    public TextBlockVersion() {
        super();
    }

    public TextBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    @Override
    public double compareTo(PostBlockVersion otherBlock) {
        try {
            return similarityMetric.apply(getContent(), otherBlock.getContent());
        } catch (InputTooShortException e) {
            return backupSimilarityMetric.apply(getContent(), otherBlock.getContent());
        }
    }

    @Override
    public String toString() {
        return "TextBlockVersion: " + getContent();
    }
}
