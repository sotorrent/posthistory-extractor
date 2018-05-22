package org.sotorrent.posthistoryextractor.urls;

import javax.persistence.*;

@Entity
@Table(name="PostReferenceGH")
public class PostReferenceGH {
    private int id;
    private String fileId;
    private String repoName;
    private String branch;
    private String path;
    private String fileExt;
    private Integer size;
    private Integer copies;
    private Integer postId;
    private Integer postTypeId;
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
    @Column(name = "FileId")
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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
    @Column(name = "Branch")
    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Basic
    @Column(name = "Path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Basic
    @Column(name = "FileExt")
    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    @Basic
    @Column(name = "Size")
    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Basic
    @Column(name = "Copies")
    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
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
    @Column(name = "PostTypeId")
    public Integer getPostTypeId() {
        return postTypeId;
    }

    public void setPostTypeId(Integer postTypeId) {
        this.postTypeId = postTypeId;
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
