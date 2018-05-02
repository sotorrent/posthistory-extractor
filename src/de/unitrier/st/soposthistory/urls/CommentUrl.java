package de.unitrier.st.soposthistory.urls;

import javax.persistence.*;

@Entity
@Table(name="CommentUrl")
public class CommentUrl{
    private int id;
    private Integer postId;
    private Integer commentId;
    private String protocol;
    private String completeDomain;
    private String rootDomain;
    private String path;
    private String url;

    public CommentUrl(){
        this.commentId = null;
        this.protocol = null;
        this.completeDomain = null;
        this.rootDomain = null;
        this.path = null;
        this.url = null;
    }

    public CommentUrl(int postId, int commentId, Link link){
        this.postId = postId;
        this.commentId = commentId;
        this.protocol = link.getProtocol();
        this.completeDomain = link.getCompleteDomain();
        this.rootDomain = link.getRootDomain();
        this.path = link.getPath();
        this.url = link.getUrl();
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
    @Column(name = "CommentId")
    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    @Basic
    @Column(name = "Protocol")
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Basic
    @Column(name = "CompleteDomain")
    public String getCompleteDomain() {
        return completeDomain;
    }

    public void setCompleteDomain(String completeDomain) {
        this.completeDomain = completeDomain;
    }

    @Basic
    @Column(name = "RootDomain")
    public String getRootDomain() {
        return rootDomain;
    }

    public void setRootDomain(String rootDomain) {
        this.rootDomain = rootDomain;
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