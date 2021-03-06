package org.sotorrent.posthistoryextractor.version;

import org.sotorrent.posthistoryextractor.history.Posts;

import javax.persistence.*;
import java.sql.Timestamp;

import static org.sotorrent.stringsimilarity.edit.Base.levenshteinDistance;

@Entity
@Table(name="TitleVersion")
public class TitleVersion extends Version {
    // database
    // see superclass members
    private Integer predPostHistoryId;
    private Integer predEditDistance;
    private Integer succPostHistoryId;
    private Integer succEditDistance;
    private String title;
    // internal
    private TitleVersion pred;
    private TitleVersion succ;

    public TitleVersion() {
        // database
        this.postId = null;
        this.postTypeId = null;
        this.postHistoryId = null;
        this.postHistoryTypeId = null;
        this.creationDate = null;
        this.title = null;
        this.predPostHistoryId = null;
        this.succPostHistoryId = null;
        this.mostRecentVersion = false;
        // internal
        this.pred = null;
        this.succ = null;
    }

    public TitleVersion(Integer postId, Integer postHistoryId, Byte postTypeId, Byte postHistoryTypeId,
                   Timestamp creationDate, String title) {
        this();
        this.postId = postId;
        setPostTypeId(postTypeId);
        this.postHistoryId = postHistoryId;
        this.postHistoryTypeId = postHistoryTypeId;
        this.creationDate = creationDate;
        this.title = title;
    }

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "PostId")
    @Override
    public Integer getPostId() {
        return postId;
    }

    @Override
    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "PostTypeId")
    @Override
    public Byte getPostTypeId() {
        return postTypeId;
    }

    @Override
    public void setPostTypeId(Byte postTypeId) {
        if (postTypeId != Posts.QUESTION_ID) {
            throw new IllegalArgumentException("Title versions can only exist for questions. PostTypeId was "
                    + postTypeId + " for post " + postId + ".");
        }
        this.postTypeId = postTypeId;
    }

    @Basic
    @Column(name = "PostHistoryId")
    @Override
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    @Override
    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    @Basic
    @Column(name = "PostHistoryTypeId")
    @Override
    public Byte getPostHistoryTypeId() {
        return postHistoryTypeId;
    }

    @Override
    public void setPostHistoryTypeId(Byte postHistoryTypeId) {
        this.postHistoryTypeId = postHistoryTypeId;
    }

    @Basic
    @Column(name = "CreationDate")
    @Override
    public Timestamp getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    @Basic
    @Column(name = "Title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "PredPostHistoryId")
    public Integer getPredPostHistoryId() {
        return predPostHistoryId;
    }

    public void setPredPostHistoryId(Integer predPostHistoryId) {
        this.predPostHistoryId = predPostHistoryId;
    }

    @Basic
    @Column(name = "PredEditDistance")
    public Integer getPredEditDistance() {
        return predEditDistance;
    }

    public void setPredEditDistance(Integer predEditDistance) {
        this.predEditDistance = predEditDistance;
    }

    @Basic
    @Column(name = "SuccPostHistoryId")
    public Integer getSuccPostHistoryId() {
        return succPostHistoryId;
    }

    public void setSuccPostHistoryId(Integer succPostHistoryId) {
        this.succPostHistoryId = succPostHistoryId;
    }

    @Basic
    @Column(name = "SuccEditDistance")
    public Integer getSuccEditDistance() {
        return succEditDistance;
    }

    public void setSuccEditDistance(Integer succEditDistance) {
        this.succEditDistance = succEditDistance;
    }

    @Basic
    @Column(name = "MostRecentVersion")
    public boolean isMostRecentVersion() {
        return mostRecentVersion;
    }

    public void setMostRecentVersion(boolean mostRecentVersion) {
        this.mostRecentVersion = mostRecentVersion;
    }

    @Transient
    public TitleVersion getPred() {
        return pred;
    }

    public void setPred(TitleVersion pred) {
        this.pred = pred;
    }

    @Transient
    public TitleVersion getSucc() {
        return succ;
    }

    public void setSucc(TitleVersion succ) {
        this.succ = succ;
    }

    public void setEditDistance() {
        if (pred != null) {
            predEditDistance = levenshteinDistance(title, pred.getTitle());
        }
        if (succ != null) {
            succEditDistance = levenshteinDistance(title, succ.getTitle());
        }
    }

    @Override
    public String toString() {
        return "TitleVersion: PostId=" + postId + ", PostHistoryId=" + postHistoryId;
    }
}
