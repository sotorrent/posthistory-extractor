package org.sotorrent.posthistoryextractor.comments;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.sotorrent.posthistoryextractor.urls.CommentUrl;
import org.sotorrent.posthistoryextractor.urls.Link;
import org.hibernate.StatelessSession;
import org.sotorrent.util.FileUtils;
import org.sotorrent.util.HibernateUtils;
import org.sotorrent.util.LogUtils;

import javax.persistence.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

@Entity
@Table(name="Comments")
public class Comments {
    private static Logger logger = null;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(Comments.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final CSVFormat csvFormat;
    static {
        // configure CSV format for ground truth
        csvFormat = CSVFormat.DEFAULT
                .withHeader("Id", "PostId", "Score", "Text", "CreationDate", "UserDisplayName", "UserId")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withFirstRecordAsHeader();
    }

    // database
    private int id;
    private int postId;
    private int score;
    private String text;
    private Timestamp creationDate;
    private String userDisplayName;
    private int userId;

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

    @Basic
    @Column(name = "CreationDate")
    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    @Basic
    @Column(name = "UserDisplayName")
    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    @Basic
    @Column(name = "UserId")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void extractUrls() {
        // Comments.Text may be null according to database schema
        if (text == null) {
            return;
        }

        List<Link> extractedLinks = Link.extractTyped(text);
        for (Link currentLink : extractedLinks) {
            urls.add(new CommentUrl(postId, id, currentLink, text));
        }
    }

    void insertUrls(StatelessSession session) {
        HibernateUtils.insertList(session, urls);
    }

    @Transient
    public List<CommentUrl> getExtractedUrls() {
        return urls;
    }

    public static Comments readFromCSV(Path dir, int commentId) {
        // ensure that input directory exists
        try {
            FileUtils.ensureDirectoryExists(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Comments comment = null;
        Path pathToCSVFile = Paths.get(dir.toString(), commentId + ".csv");

        CSVParser parser;
        try {
            parser = CSVParser.parse(
                    pathToCSVFile.toFile(),
                    StandardCharsets.UTF_8,
                    Comments.csvFormat
            );
            parser.getHeaderMap();

            logger.info("Reading version data from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            List<CSVRecord> records = parser.getRecords();

            if (records.size() > 0) {
                for (CSVRecord record : records) {
                    int id = Integer.parseInt(record.get("Id"));
                    int postId = Integer.parseInt(record.get("PostId"));
                    int score = Integer.parseInt(record.get("Score"));
                    String text = record.get("Text");
                    Timestamp creationDate = Timestamp.valueOf(record.get("CreationDate"));
                    String userDisplayName = record.get("UserDisplayName");
                    Integer userId;
                    try {
                        userId = Integer.parseInt(record.get("UserId"));
                    } catch (NumberFormatException e) {
                        userId = null;
                    }

                    comment = new Comments();
                    comment.setId(id);
                    comment.setPostId(postId);
                    comment.setScore(score);
                    comment.setText(text);
                    comment.setCreationDate(creationDate);
                    comment.setUserDisplayName(userDisplayName);
                    comment.setUserId(userId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (comment == null) {
            throw new IllegalArgumentException("Reading comment from CSV failed");
        }

        return comment;
    }
}
