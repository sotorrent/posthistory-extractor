package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.stringsimilarity.util.InputTooShortException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.List;
import java.util.function.BiFunction;

@Entity
@DiscriminatorValue(value="2")
public class CodeBlockVersion extends PostBlockVersion {
    // TODO: Aus Tags der Frage (und evtl. Analyse des Codes) die Programmiersprache ableiten

    public static final int postBlockTypeId = 2;
    public static BiFunction<String, String, Double> similarityMetric = de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap; //TODO: update this after evaluation
    public static BiFunction<String, String, Double> backupSimilarityMetric = de.unitrier.st.stringsimilarity.edit.Variants::levenshtein; // TODO: use best edit-based metric here
    public static double similarityThreshold = 0.6; //TODO: update this after evaluation (two thresholds needed because of backup)

    public CodeBlockVersion() {
        super();
    }

    public CodeBlockVersion(int postId, int postHistoryId) {
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
    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks) {
        return super.findMatchingPredecessors(previousVersionPostBlocks, similarityThreshold);
    }

    @Override
    public String toString() {
        return "CodeBlockVersion: " + getContent();
    }
}
