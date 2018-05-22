package org.sotorrent.posthistoryextractor.diffs;

import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;
import org.hibernate.StatelessSession;
import org.sotorrent.util.HibernateUtils;

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
                        block.getPostHistoryId(), block.getLocalId(), block.getId(),
                        block.getPredPostHistoryId(), block.getPredLocalId(), block.getPredPostBlockVersionId(),
                        LineDiff.operationToInt(diff.operation), diff.text
                    );
                    this.add(diffPrev);
                }
            }
        }
    }

    public void insert(StatelessSession session) {
        HibernateUtils.insertList(session, this);
    }
}
