package de.unitrier.st.soposthistory.diffs;

import javax.persistence.*;

@Entity
@Table(name="PostBlockDiff")
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
}
