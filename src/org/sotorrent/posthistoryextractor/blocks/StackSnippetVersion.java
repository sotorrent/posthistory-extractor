package org.sotorrent.posthistoryextractor.blocks;

import javax.persistence.*;

@Entity
@Table(name="StackSnippetVersion")
public class StackSnippetVersion {
    // database
    private int id;
    private int postId;
    private byte postTypeId;
    private int postHistoryId;
    private String content;
    // internal
    private StringBuilder contentBuilder;

    public StackSnippetVersion() {
        // internal
        this.contentBuilder = new StringBuilder();
    }

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(name = "PostTypeId")
    public Byte getPostTypeId() {
        return postTypeId;
    }

    public void setPostTypeId(Byte postTypeId) {
        this.postTypeId = postTypeId;
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
    @Column(name = "Content")
    public String getContent() {
        return content == null ? contentBuilder.toString() : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void append(String line) {
        if (contentBuilder.length() > 0) {
            // end previous line with line break
            contentBuilder.append("\n");
        }

        if (line.length() == 0) {
            contentBuilder.append("\n");
        } else {
            contentBuilder.append(line);
        }
    }
}
