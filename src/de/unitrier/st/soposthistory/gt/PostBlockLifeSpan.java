package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;

import java.util.LinkedList;

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

    public int getPostId() {
        return postId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    public boolean equals(PostBlockLifeSpan other) {
        if (this.size() != other.size()) {
            return false;
        }

        for (int i=0; i<this.size(); i++) {
            if (!this.get(i).equals(other.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (PostBlockLifeSpanVersion version : this) {
            sb.append(version);
            sb.append("\n");
        }
        return "PostBlockLifeSpan (PostId=" + postId + "; PostBlockTypeId=" + postBlockTypeId + "):\n" + sb;
    }
}
