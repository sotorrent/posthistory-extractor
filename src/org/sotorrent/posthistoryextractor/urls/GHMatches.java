package org.sotorrent.posthistoryextractor.urls;

import javax.persistence.*;

@Entity
@Table(name="GHMatches")
public class GHMatches {
    private int id;
    private String fileId;
    private String postIds;
    private String matchedLine;

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "FileId")
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @Basic
    @Column(name = "PostIds")
    public String getPostId() {
        return postIds;
    }

    public void setPostId(String postIds) {
        this.postIds = postIds;
    }

    @Basic
    @Column(name = "MatchedLine")
    public String getMatchedLine() {
        return matchedLine;
    }

    public void setMatchedLine(String matchedLine) {
        this.matchedLine = matchedLine;
    }
}
