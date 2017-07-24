package de.unitrier.st.soposthistory.urls;

import javax.persistence.*;

@Entity
@Table(name = "PostVersionUrl", schema = "stackoverflow16_12")
public class PostVersionUrl{
    private int id;
    private Integer postHistoryId;
    private String url;

    public PostVersionUrl(){
        this.postHistoryId = null;
        this.url = null;
    }

    public PostVersionUrl(int postHistoryId, String urls){
        this.postHistoryId = postHistoryId;
        this.url = urls;
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
    @Column(name = "PostHistoryId")
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
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
