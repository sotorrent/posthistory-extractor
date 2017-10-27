package de.unitrier.st.soposthistory.gt;

public class GroundTruthBlock {
    private int postId;
    private int postHistoryId;
    private int postBlockTypeId;
    private int localId;
    private Integer predLocalId;
    private Integer succLocalId;
    private String comment;

    public GroundTruthBlock(int postId, int postHistoryId, int postBlockTypeId, int localId, Integer predLocalId,
                            Integer succLocalId, String comment) {
        this.postId = postId;
        this.postHistoryId = postHistoryId;
        this.postBlockTypeId = postBlockTypeId;
        this.localId = localId;
        this.predLocalId = predLocalId;
        this.succLocalId = succLocalId;
        this.comment = comment;
    }

    public int getPostId() {
        return postId;
    }

    public int getPostHistoryId() {
        return postHistoryId;
    }

    public int getPostBlockTypeId() {
        return postBlockTypeId;
    }

    public int getLocalId() {
        return localId;
    }

    public Integer getPredLocalId() {
        return predLocalId;
    }

    public Integer getSuccLocalId() {
        return succLocalId;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return postId + ";" + postHistoryId + ";" + postBlockTypeId + ";" + localId + ";" + predLocalId + ";"
                + succLocalId + ";" + comment;
    }
}
