package de.unitrier.st.soposthistory.diffs;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import de.unitrier.st.util.Util;
import org.hibernate.StatelessSession;

import java.util.LinkedList;
import java.util.List;

public class PostBlockDiffList extends LinkedList<PostBlockDiff> {

    public void fromPostVersionList(PostVersionList versionList) {
        for (PostVersion version : versionList) {
            for (PostBlockVersion block : version.getPostBlocks()) {
                List<diff_match_patch.Diff> predDiff = block.getPredDiff();
                if (predDiff == null) {
                    continue;
                }
                for (diff_match_patch.Diff diff : predDiff) {
                    PostBlockDiff diffPrev = new PostBlockDiff(
                            block.getPostId(),
                            block.getPostHistoryId(),
                            block.getLocalId(),
                            block.getPredLocalId(),
                            block.getPredPostBlockId(),
                            block.getId(),
                            LineDiff.operationToInt(diff.operation),
                            diff.text
                    );
                    this.add(diffPrev);
                }
            }
        }
    }

    public void insert(StatelessSession session) {
        Util.insertList(session, this);
    }
}
