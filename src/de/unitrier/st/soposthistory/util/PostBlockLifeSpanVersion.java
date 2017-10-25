package de.unitrier.st.soposthistory.util;

public class PostBlockLifeSpanVersion {
    private int postId;
    private int postHistoryId;
    private int postBlockTypeId;
    private int version;
    private int localId;
    private Integer predLocalId;
    private Integer succLocalId;
    private String comment;

    public PostBlockLifeSpanVersion(int postId, int postHistoryId, int postBlockTypeId, int version, int localId) {
        this(postId, postHistoryId, postBlockTypeId, version, localId, null, null);
    }

    public PostBlockLifeSpanVersion(int postId, int postHistoryId, int postBlockTypeId, int version, int localId, Integer predLocalId, Integer succLocalId) {
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postBlockTypeId = postBlockTypeId;
        this.version = version;
        this.localId = localId;
        this.predLocalId = predLocalId;
        this.succLocalId = succLocalId;
        this.comment = "";
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

    public Integer getPostBlockTypeId() {
        return postBlockTypeId;
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof PostBlockLifeSpanVersion) {
            PostBlockLifeSpanVersion other = (PostBlockLifeSpanVersion) o;
            return (this.version == other.version
                    && this.postBlockTypeId == other.postBlockTypeId
                    && this.localId == other.localId
                    && this.postId == other.postId);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "(" + version + "," + localId + ")";
    }

}
