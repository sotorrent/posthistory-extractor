package de.unitrier.st.soposthistory.diffs;

import javax.persistence.*;

@Entity
@Table(name="PostBlockDiff")
public class PostBlockDiff {
    private int id;
    private int postId;
    private int postHistoryId;
    private int postBlockVersionId;
    private int localId;
    private int predPostHistoryId;
    private int predLocalId;
    private int predPostBlockVersionId;
    private int postBlockDiffOperationId;
    private String text;

    public PostBlockDiff() {}

    public PostBlockDiff(int postId,
                         int postHistoryId, int localId, int postBlockVersionId,
                         int predPostHistoryId, int predLocalId, int predPostBlockVersionId,
                         int postBlockDiffOperationId, String text) {
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.localId = localId;
        this.postBlockVersionId = postBlockVersionId;
        this.predPostHistoryId = predPostHistoryId;
        this.predLocalId = predLocalId;
        this.predPostBlockVersionId = predPostBlockVersionId;
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
    @Column(name = "LocalId")
    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
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
    @Column(name = "PredPostHistoryId")
    public int getPredPostHistoryId() {
        return predPostHistoryId;
    }

    public void setPredPostHistoryId(int predPostHistoryId) {
        this.predPostHistoryId = predPostHistoryId;
    }

    @Basic
    @Column(name = "PredLocalId")
    public int getPredLocalId() {
        return predLocalId;
    }

    public void setPredLocalId(int predLocalId) {
        this.predLocalId = predLocalId;
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
