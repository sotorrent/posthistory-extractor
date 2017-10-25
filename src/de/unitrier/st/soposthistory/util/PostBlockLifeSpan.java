package de.unitrier.st.soposthistory.util;

import java.util.LinkedList;
import java.util.List;

public class PostBlockLifeSpan extends LinkedList<PostBlockLifeSpanVersion> {
    private int postId;
    private int postBlockTypeId;

    public PostBlockLifeSpan(int postId, int postBlockTypeId) {
        this.postId = postId;
        this.postBlockTypeId = postBlockTypeId;
    }


    public static int getVersionCount(List<PostBlockLifeSpan> postBlockLifeSpans) {
        int versionCount = 0;
        for (PostBlockLifeSpan postBlockLifeSpan : postBlockLifeSpans) {
            versionCount += postBlockLifeSpan.size();
        }
        return versionCount;
    }

    public static String toString(LinkedList<PostBlockLifeSpan> postBlockLifeSpans) {
        StringBuilder sb = new StringBuilder();
        sb.append("number of snapshots: ");
        sb.append(getVersionCount(postBlockLifeSpans));
        sb.append("\n");

        for (PostBlockLifeSpan postBlockLifeSpan : postBlockLifeSpans) {
            sb.append(postBlockLifeSpan);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PostBlockLifeSpanVersion snapshot : this) {
            sb.append(snapshot);
        }
        return postBlockTypeId + ": " + sb + "\n";
    }
}
