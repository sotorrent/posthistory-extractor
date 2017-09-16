package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.stringsimilarity.set.Variants;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.function.BiFunction;

@Entity
@DiscriminatorValue(value="1")
public class TextBlockVersion extends PostBlockVersion {
    // TODO: handling of URL blocks (tranform to inline URLs, see AnchorTextAndUrlHandler)

    public static final int blockTypeId = 1;
    public static BiFunction<String, String, Double> similarityMetric = Variants::fourGramOverlap; //TODO: update this after evaluation
    public static double similarityThreshold = 0.6; //TODO: update this after evaluation

    public TextBlockVersion() {
        super();
    }

    public TextBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    @Override
    public double compareTo(PostBlockVersion otherBlock) {
        return similarityMetric.apply(getContent(), otherBlock.getContent());
    }

    @Override
    public String toString() {
        return "TextBlockVersion: " + getContent();
    }
}
