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
    public double compareTo(PostBlockVersion otherBlock, Config config) {
        setSimilarityThreshold(config.getCodeSimilarityThreshold());
        return compareTo(otherBlock, config.getCodeSimilarityMetric(), config.getCodeBackupSimilarityMetric());
    }

    @Override
    public String toString() {
        return "CodeBlockVersion: " + getContent();
    }
}
