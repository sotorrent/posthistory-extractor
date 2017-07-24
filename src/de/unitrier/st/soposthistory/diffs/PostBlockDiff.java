package de.unitrier.st.soposthistory.diffs;

import javax.persistence.*;

@Entity
@Table(name = "PostBlockDiff", schema = "stackoverflow16_12")
public class PostBlockDiff {
    private int id;
    private int postId;
    private int postHistoryId;
    private int predPostBlockVersionId;
    private int postBlockVersionId;
    private int postBlockDiffOperationId;
    private String text;

    public PostBlockDiff() {}

    public PostBlockDiff(int postId, int postHistoryId,
                         int predPostBlockVersionId, int postBlockVersionId,
                         int postBlockDiffOperationId, String text) {
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.predPostBlockVersionId = predPostBlockVersionId;
        this.postBlockVersionId = postBlockVersionId;
        this.postBlockDiffOperationId = postBlockDiffOperationId;
        this.text = text;
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
    public int getPostId() {
        return postId;
    }

    public void setPostId(int postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "PostHistoryId")
    public int getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(int postHistoryId) {
        this.postHistoryId = postHistoryId;
    }

    @Basic
    @Column(name = "PredPostBlockVersionId")
    public int getPredPostBlockVersionId() {
        return predPostBlockVersionId;
    }

    public void setPredPostBlockVersionId(int predPostBlockVersionId) {
        this.predPostBlockVersionId = predPostBlockVersionId;
    }

    @Basic
    @Column(name = "PostBlockVersionId")
    public int getPostBlockVersionId() {
        return postBlockVersionId;
    }

    public void setPostBlockVersionId(int postBlockVersionId) {
        this.postBlockVersionId = postBlockVersionId;
    }

    @Basic
    @Column(name = "PostBlockDiffOperationId")
    public int getPostBlockDiffOperationId() {
        return postBlockDiffOperationId;
    }

    public void setPostBlockDiffOperationId(int postBlockDiffOperationId) {
        this.postBlockDiffOperationId = postBlockDiffOperationId;
    }

    @Basic
    @Column(name = "Text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBlockDiff that = (PostBlockDiff) o;

        if (id != that.getId()) return false;
        if (postId != that.getPostId()) return false;
        if (postHistoryId != that.getPostHistoryId()) return false;
        if (postBlockVersionId != that.getPostBlockVersionId()) return false;
        if (postBlockDiffOperationId != that.getPostBlockDiffOperationId()) return false;
        if (text != null ? !text.equals(that.text) : that.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + postId;
        result = 31 * result + postHistoryId;
        result = 31 * result + predPostBlockVersionId;
        result = 31 * result + postBlockVersionId;
        result = 31 * result + postBlockDiffOperationId;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
