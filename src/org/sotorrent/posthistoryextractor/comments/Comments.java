package org.sotorrent.posthistoryextractor.comments;

import org.sotorrent.posthistoryextractor.urls.CommentUrl;
import org.sotorrent.posthistoryextractor.urls.Link;
import org.hibernate.StatelessSession;
import org.sotorrent.util.HibernateUtils;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name="Comments")
public class Comments {
    // database
    private int id;
    private int postId;
    private int score;
    private String text;
    // internal
    private List<CommentUrl> urls;

    public Comments() {
        this.urls = new LinkedList<>();
    }

    @Id
    @Column(name = "Id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "PostId")
    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "Score")
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Basic
    @Column(name = "Text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    void extractUrls() {
        // Comments.Text may be null according to database schema
        if (text == null) {
            return;
        }

        List<Link> extractedLinks = Link.extractBare(text);
        for (Link currentLink : extractedLinks) {
            urls.add(new CommentUrl(postId, id, currentLink));
        }
    }

    void insertUrls(StatelessSession session) {
        HibernateUtils.insertList(session, urls);
    }
}