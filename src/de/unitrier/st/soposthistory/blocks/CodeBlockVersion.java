package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.stringsimilarity.set.Variants;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.function.BiFunction;

@Entity
@DiscriminatorValue(value="2")
public class CodeBlockVersion extends PostBlockVersion {
    // TODO: Aus Tags der Frage (und evtl. Analyse des Codes) die Programmiersprache ableiten

    public static final int blockTypeId = 2;
    public static BiFunction<String, String, Double> similarityMetric = Variants::twoGramDice; //TODO: update this after evaluation
    public static double similarityThreshold = 0.6; //TODO: update this after evaluation

    public CodeBlockVersion() {
        super();
    }

    public CodeBlockVersion(int postId, int postHistoryId) {
        super(postId, postHistoryId);
    }

    @Override
    public double compareTo(PostBlockVersion otherBlock) {
        return similarityMetric.apply(getContent(), otherBlock.getContent());
    }

    @Override
    public String toString() {
        return "CodeBlockVersion: " + getContent();
    }
}
