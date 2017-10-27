package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;

import java.util.LinkedList;
import java.util.List;

public class PostBlockLifeSpan extends LinkedList<PostBlockLifeSpanVersion> {
    private int postId;
    private int postBlockTypeId;

    public PostBlockLifeSpan(int postId, int postBlockTypeId) {
        this.postId = postId;
        this.postBlockTypeId = postBlockTypeId;
    }

    public static PostBlockLifeSpan fromPostBlockVersion(PostBlockVersion firstVersion) {
        if (firstVersion.isProcessed()) {
            throw new IllegalArgumentException("PostBlockVersion has already been processed: " + firstVersion);
        }

        int postIdFirst = firstVersion.getPostId();
        int postBlockTypeIdFirst = firstVersion instanceof TextBlockVersion ? TextBlockVersion.postBlockTypeId : CodeBlockVersion.postBlockTypeId;

        PostBlockLifeSpan lifeSpan = new PostBlockLifeSpan(postIdFirst, postBlockTypeIdFirst);
        PostBlockVersion currentVersion = firstVersion;

        int versionCount = 1;
        while (currentVersion != null) {
            int postId = currentVersion.getPostId();
            if (postId != postIdFirst) {
                throw new IllegalStateException("PostIds in life span do not match.");
            }
            int postBlockTypeId = currentVersion instanceof TextBlockVersion ? TextBlockVersion.postBlockTypeId : CodeBlockVersion.postBlockTypeId;
            if (postBlockTypeId != postBlockTypeIdFirst) {
                throw new IllegalStateException("PostBlockTypeIds in life span do not match.");
            }
            int postHistoryId = currentVersion.getPostHistoryId();
            int localId = currentVersion.getLocalId();

            PostBlockLifeSpanVersion newLifeSpanVersion = new PostBlockLifeSpanVersion(
                    postId, postHistoryId, postBlockTypeId, localId
            );
            newLifeSpanVersion.setVersion(versionCount);
            versionCount++;

            if (currentVersion.getPred() != null) {
                newLifeSpanVersion.setPredLocalId(currentVersion.getPred().getLocalId());
            }
            if (currentVersion.getSucc() != null) {
                newLifeSpanVersion.setSuccLocalId(currentVersion.getSucc().getLocalId());
            }

            lifeSpan.add(newLifeSpanVersion);
            currentVersion.setProcessed(true);
            currentVersion = currentVersion.getSucc();
        }

        return lifeSpan;
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

    public int getPostId() {
        return postId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PostBlockLifeSpanVersion version : this) {
            sb.append(version);
        }
        return postBlockTypeId + ": " + sb + "\n";
    }
}
