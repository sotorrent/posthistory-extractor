package de.unitrier.st.soposthistory.urls;

import javax.persistence.*;

@Entity
@Table(name="PostVersionUrl")
public class PostVersionUrl{
    private int id;
    private Integer postId;
    private Integer postHistoryId;
    private Integer postBlockVersionId;
    private String domain;
    private String url;

    public PostVersionUrl(){
        this.postHistoryId = null;
        this.domain = null;
        this.url = null;
    }

    public PostVersionUrl(int postId, int postHistoryId, int postBlockVersionId, String domain, String url){
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postBlockVersionId = postBlockVersionId;
        this.domain = domain;
        this.url = url;
    }

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
    @Column(name = "PostId")
    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "PostHistoryId")
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    @Basic
    @Column(name = "PostBlockVersionId")
    public Integer getPostBlockVersionId() {
        return postBlockVersionId;
    }

    public void setPostBlockVersionId(Integer postBlockVersionId) {
        this.postBlockVersionId = postBlockVersionId;
    }

    @Basic
    @Column(name = "Domain")
    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Basic
    @Column(name = "Url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Url: " + url + "\n";
    }
}
