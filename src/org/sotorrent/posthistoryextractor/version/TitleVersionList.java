package org.sotorrent.posthistoryextractor.version;

import org.sotorrent.posthistoryextractor.history.PostHistory;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.hibernate.StatelessSession;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class TitleVersionList extends LinkedList<TitleVersion> implements VersionList {
    private int postId;
    private byte postTypeId;
    private boolean sorted;

    public TitleVersionList(int postId, byte postTypeId) {
        super();
        if (postTypeId != Posts.QUESTION_ID) {
            throw new IllegalArgumentException("Title versions can only exist for questions (expected id: "
                    + Posts.QUESTION_ID + "; actual id: " + postTypeId + ")");
        }
        this.postId = postId;
        this.postTypeId = postTypeId;
        this.sorted = false;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void processVersionHistory() {
        // list must be sorted (in particular the pred and succ references must be set)
        if (!this.isSorted()) {
            this.sort();
        }
    }

    public void sort() {
        // empty list is already sorted
        if (this.size() == 0) {
            return;
        }

        // sort versions according to their creation date
        this.sort(Comparator.comparing(TitleVersion::getCreationDate));

        // set predecessors and successors, set edit distance
        for (int i=1; i<this.size(); i++) {
            TitleVersion currentVersion = this.get(i);
            TitleVersion pred = this.get(i-1);
            currentVersion.setPred(pred);
            currentVersion.setPredPostHistoryId(pred.getPostHistoryId());
            pred.setSucc(currentVersion);
            pred.setSuccPostHistoryId(currentVersion.getPostHistoryId());
            currentVersion.setEditDistance();
            pred.setEditDistance();
        }
        this.getLast().setEditDistance(); // this element exists, because of size check above

        this.sorted = true;

        // mark most recent post version and post block versions
        this.getLast().setMostRecentVersion(true);
    }

    public void insert(StatelessSession session) {
        for (TitleVersion currentVersion : this) {
            // save current title version
            session.insert(currentVersion);
        }
    }

    public static TitleVersionList readFromCSV(Path dir, int postId) {
        return readFromCSV(dir, postId, true);
    }

    public static TitleVersionList readFromCSV(Path dir, int postId, boolean processVersionHistory) {
        // read post history
        List<PostHistory> postHistoryList = PostHistory.readFromCSV(dir, postId, PostHistory.titlePostHistoryTypes);

        // convert to title version list
        TitleVersionList titleVersionList = new TitleVersionList(postId, Posts.QUESTION_ID);
        for (PostHistory postHistory : postHistoryList) {
            titleVersionList.add(postHistory.toTitleVersion(Posts.QUESTION_ID));
        }

        // sort list according to CreationDate, because order in CSV may not be chronologically
        titleVersionList.sort();

        if (processVersionHistory) {
            titleVersionList.processVersionHistory();
        }

        return titleVersionList;
    }

    @Override
    public int getPostId() {
        return postId;
    }

    @Override
    public byte getPostTypeId() {
        return postTypeId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TitleVersion version : this) {
            sb.append(version.toString());
            sb.append("\n");
        }
        return "TitleVersionList (PostId=" + postId + "):\n" + sb;
    }
}
