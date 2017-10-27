package de.unitrier.st.soposthistory.gt;

import java.util.Set;

public class PostBlockLifeSpanVersion {
    // data in CSV
    private int postId;
    private int postHistoryId;
    private int postBlockTypeId;
    private int localId;
    private Integer predLocalId;
    private Integer succLocalId;
    private String comment;
    // not in CSV
    private int version;
    // internal
    private boolean processed;

    public PostBlockLifeSpanVersion(int postId, int postHistoryId, int postBlockTypeId, int localId) {
        this(postId, postHistoryId, postBlockTypeId, localId, null, null, "", -1);
    }

    public PostBlockLifeSpanVersion(int postId, int postHistoryId, int postBlockTypeId, int localId,
                                    Integer predLocalId, Integer succLocalId, String comment) {
        this(postId, postHistoryId, postBlockTypeId, localId, predLocalId, succLocalId, comment, -1);
    }

    public PostBlockLifeSpanVersion(int postId, int postHistoryId, int postBlockTypeId, int localId,
                                    Integer predLocalId, Integer succLocalId, String comment, int version) {
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postBlockTypeId = postBlockTypeId;
        this.localId = localId;
        this.predLocalId = predLocalId;
        this.succLocalId = succLocalId;
        this.comment = comment;
        // not in CSV
        this.version = version;
        // internal
        this.processed = false;
    }

    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    public int getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(int postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    public void setPostBlockTypeId(int postBlockTypeId) {
        this.postBlockTypeId = postBlockTypeId;
    }

    public void setPostBlockTypeId(Integer postBlockTypeId) {
        this.postBlockTypeId = postBlockTypeId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public Integer getPredLocalId() {
        return predLocalId;
    }

    public void setPredLocalId(Integer predLocalId) {
        this.predLocalId = predLocalId;
    }

    public Integer getSuccLocalId() {
        return succLocalId;
    }

    public void setSuccLocalId(Integer succLocalId) {
        this.succLocalId = succLocalId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isSelected(Set<Integer> postBlockTypeFilter) {
        return postBlockTypeFilter.contains(postBlockTypeId);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PostBlockLifeSpanVersion) {
            PostBlockLifeSpanVersion other = (PostBlockLifeSpanVersion) o;
            return (this.postId == other.postId
                    && this.postHistoryId == other.postHistoryId
                    && this.postBlockTypeId == other.postBlockTypeId
                    && this.localId == other.localId
                    && this.version == other.version
            );
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return postId + ";" + postHistoryId + ";" + postBlockTypeId + ";" + localId + ";" + predLocalId + ";"
                + succLocalId + ";" + comment + ";" + version;
    }

}
