package org.sotorrent.posthistoryextractor.urls;

import javax.persistence.*;

@Entity
@Table(name="CommentUrl")
public class CommentUrl{
    private int id;
    private Integer postId;
    private Integer commentId;
    private String linkType;
    private String linkPosition;
    private String linkAnchor;
    private String protocol;
    private String rootDomain;
    private String completeDomain;
    private String path;
    private String query;
    private String fragmentIdentifier;
    private String url;
    private String fullMatch;

    public CommentUrl(){}

    public CommentUrl(int postId, int commentId, Link link, String markdownContent){
        this.postId = postId;
        this.commentId = commentId;
        this.linkType = link.getType();
        this.linkPosition = link.getPosition(markdownContent);
        this.linkAnchor = link.getAnchor();
        this.protocol = link.getUrlObject().getProtocol();
        this.rootDomain = link.getUrlObject().getRootDomain();
        this.completeDomain = link.getUrlObject().getCompleteDomain();
        this.path = link.getUrlObject().getPath();
        this.query = link.getUrlObject().getQuery();
        this.fragmentIdentifier = link.getUrlObject().getFragmentIdentifier();
        this.url = link.getUrlString();
        this.fullMatch = link.getFullMatch();
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
    @Column(name = "LinkType")
    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    @Basic
    @Column(name = "LinkPosition")
    public String getLinkPosition() {
        return linkPosition;
    }

    public void setLinkPosition(String linkPosition) {
        this.linkPosition = linkPosition;
    }

    @Basic
    @Column(name = "LinkAnchor")
    public String getLinkAnchor() {
        return linkAnchor;
    }

    public void setLinkAnchor(String linkAnchor) {
        this.linkAnchor = linkAnchor;
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
    @Column(name = "RootDomain")
    public String getRootDomain() {
        return rootDomain;
    }

    public void setRootDomain(String rootDomain) {
        this.rootDomain = rootDomain;
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
    @Column(name = "Path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Basic
    @Column(name = "Query")
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Basic
    @Column(name = "FragmentIdentifier")
    public String getFragmentIdentifier() {
        return fragmentIdentifier;
    }

    public void setFragmentIdentifier(String fragmentIdentifier) {
        this.fragmentIdentifier = fragmentIdentifier;
    }

    @Basic
    @Column(name = "Url")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Basic
    @Column(name = "FullMatch")
    public String getFullMatch() {
        return fullMatch;
    }

    public void setFullMatch(String fullMatch) {
        this.fullMatch = fullMatch;
    }

    @Override
    public String toString() {
        return "Url: " + url + "\n";
    }
}