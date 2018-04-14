package de.unitrier.st.soposthistory.version;

import de.unitrier.st.soposthistory.history.PostHistory;
import de.unitrier.st.soposthistory.history.Posts;
import de.unitrier.st.util.Util;
import org.hibernate.StatelessSession;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

public class TitleVersionList extends LinkedList<TitleVersion> {
    private static Logger logger = null;

    private int postId;
    private boolean sorted;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(TitleVersionList.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TitleVersionList(int postId) {
        super();
        this.postId = postId;
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
        this.getLast().setEditDistance();

        this.sorted = true;
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
        List<PostHistory> postHistoryList = PostHistory.readFromCSV(dir, postId, Posts.QUESTION_ID, PostHistory.titlePostHistoryTypes);

        // convert to title version list
        TitleVersionList titleVersionList = new TitleVersionList(postId);
        for (PostHistory postHistory : postHistoryList) {
            titleVersionList.add(postHistory.toTitleVersion());
        }

        // sort list according to CreationDate, because order in CSV may not be chronologically
        titleVersionList.sort();

        if (processVersionHistory) {
            titleVersionList.processVersionHistory();
        }

        return titleVersionList;
    }
}
