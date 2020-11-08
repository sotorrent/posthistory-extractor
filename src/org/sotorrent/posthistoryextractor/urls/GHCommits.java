package org.sotorrent.posthistoryextractor.urls;

import javax.persistence.*;

@Entity
@Table(name="GHCommits")
public class GHCommits {
    private int id;
    private String commitId;
    private String repo;
    private String repoOwner;
    private String repoName;
    private Integer postId;
    private Integer commentId;
    private String soUrl;
    private String ghUrl;

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
    @Column(name = "CommitId")
    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @Basic
    @Column(name = "Repo")
    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    @Basic
    @Column(name = "RepoOwner")
    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    @Basic
    @Column(name = "RepoName")
    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Basic
    @Column(name = "PostId")
    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "CommentId")
    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    @Basic
    @Column(name = "SOUrl")
    public String getSoUrl() {
        return soUrl;
    }

    public void setSoUrl(String soUrl) {
        this.soUrl = soUrl;
    }

    @Basic
    @Column(name = "GHUrl")
    public String getGhUrl() {
        return ghUrl;
    }

    public void setGhUrl(String ghUrl) {
        this.ghUrl = ghUrl;
    }
}
