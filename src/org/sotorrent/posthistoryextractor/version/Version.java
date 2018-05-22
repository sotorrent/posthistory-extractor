package org.sotorrent.posthistoryextractor.version;

import java.sql.Timestamp;

public class Version {
    // database
    protected int id;
    protected Integer postId;
    protected Byte postTypeId;
    protected Integer postHistoryId;
    protected Byte postHistoryTypeId;
    protected Timestamp creationDate;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Byte getPostTypeId() {
        return postTypeId;
    }

    public void setPostTypeId(Byte postTypeId) {
        this.postTypeId = postTypeId;
    }

    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    public Byte getPostHistoryTypeId() {
        return postHistoryTypeId;
    }

    public void setPostHistoryTypeId(Byte postHistoryTypeId) {
        this.postHistoryTypeId = postHistoryTypeId;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }
}
