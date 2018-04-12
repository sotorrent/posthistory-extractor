package de.unitrier.st.soposthistory.urls;

import javax.persistence.*;

@Entity
@Table(name="CommentUrl")
public class CommentUrl{
    private int id;
    private Integer postId;
    private Integer commentId;
    private String url;

    public CommentUrl(){
        this.commentId = null;
        this.url = null;
    }

    public CommentUrl(int postId, int commentId, String url){
        this.postId = postId;
        this.commentId = commentId;
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
    @Column(name = "CommentId")
    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
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