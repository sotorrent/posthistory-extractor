package de.unitrier.st.soposthistory.gt;

import de.unitrier.st.soposthistory.blocks.PostBlockVersion;

import java.util.*;

public class PostBlockLifeSpan extends LinkedList<PostBlockLifeSpanVersion> {
    private int postId;
    private int postBlockTypeId;
    private Map<Integer, PostBlockLifeSpanVersion> postHistoryIdToLifeSpanVersion;

    public PostBlockLifeSpan(int postId, int postBlockTypeId) {
        this.postId = postId;
        this.postBlockTypeId = postBlockTypeId;
        this.postHistoryIdToLifeSpanVersion = new HashMap<>();
    }

    public static PostBlockLifeSpan fromPostBlockVersion(PostBlockVersion firstVersion) {
        if (firstVersion.isProcessed()) {
            throw new IllegalArgumentException("PostBlockVersion has already been processed: " + firstVersion);
        }

        int postIdFirst = firstVersion.getPostId();
        int postBlockTypeIdFirst = firstVersion.getPostBlockTypeId();

        PostBlockLifeSpan lifeSpan = new PostBlockLifeSpan(postIdFirst, postBlockTypeIdFirst);
        PostBlockVersion currentVersion = firstVersion;

        while (currentVersion != null) {
            int postId = currentVersion.getPostId();
            if (postId != postIdFirst) {
                throw new IllegalStateException("PostIds in life span do not match.");
            }
            int postBlockTypeId = currentVersion.getPostBlockTypeId();
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

    @Override
    public boolean add(PostBlockLifeSpanVersion e) {
        boolean result = super.add(e);
        postHistoryIdToLifeSpanVersion.put(e.getPostHistoryId(), e);
        return result;
    }

    public int getPostId() {
        return postId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    public Set<Integer> getPostHistoryIds() {
        return postHistoryIdToLifeSpanVersion.keySet();
    }

    public PostBlockLifeSpanVersion getPostBlockLifeSpanVersion(int postHistoryId) {
        return postHistoryIdToLifeSpanVersion.get(postHistoryId);
    }

    public Set<PostBlockConnection> toPostBlockConnections() {
        Set<PostBlockConnection> postBlockConnections = new HashSet<>();
        // in a life span, all versions except the first one have predecessors
        for (int i=1; i<size(); i++) {
            postBlockConnections.add(new PostBlockConnection(get(i-1), get(i)));
        }
        return postBlockConnections;
    }

    public static Set<PostBlockConnection> toPostBlockConnections(List<PostBlockLifeSpan> lifeSpans) {
        Set<PostBlockConnection> postBlockConnections = new HashSet<>();
        for (PostBlockLifeSpan lifeSpan : lifeSpans) {
            postBlockConnections.addAll(lifeSpan.toPostBlockConnections());
        }
        return postBlockConnections;
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
