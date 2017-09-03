package de.unitrier.st.soposthistory.history;

import de.unitrier.st.soposthistory.blocks.CodeBlockVersion;
import de.unitrier.st.soposthistory.blocks.PostBlockVersion;
import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Table(name = "PostHistory", schema = "stackoverflow16_12")
public class PostHistory {
    /*
     * 2: Initial Body - The first raw body text a post is submitted with.
     *  => Frequency: 34,112,377 (2017-01-27)
     * 5: Edit Body - A post's body has been changed, the raw text is stored here as markdown.
     *  => rows with PostHistoryType 2 and 5 won't have same RevisionGUID (checked using:
     *     select RevisionGUID, PostHistoryTypeId from PostHistoryEntry_ where PostHistoryTypeId=2 or PostHistoryTypeId=5 group by RevisionGUID, PostHistoryTypeId having count(RevisionGUID)>1;)
     *  => Frequency: 17,673,423 (2017-01-27)
     * 8: Rollback Body - A post's body has reverted to a previous version - the raw text is stored here.
     *  => Comment contains RevisionGUID of old version (e.g., "Rollback to [cca4acef-243d-4c7c-ad1d-5476c94323d2] - Cleaner formatting.")
     *     (see http://meta.stackexchange.com/a/2678)
     *  => Frequency: 77,957 (2017-01-27)
     */
    public static final Set<Integer> relevantPostHistoryTypes = new HashSet<>();
    static {
        relevantPostHistoryTypes.add(2); // Initial Body
        relevantPostHistoryTypes.add(5); // Edit Body
        relevantPostHistoryTypes.add(8); // Rollback Body
    }

    // a code block is indented by four spaces or a tab (which can be preceded by spaces)
    private static Pattern codeBlockPattern = Pattern.compile("^( {4}|[ ]*\\t)");
    private static Pattern whiteSpaceLinePattern = Pattern.compile("^\\s+$");
    private static Pattern containsLetterOrDigitPattern = Pattern.compile("[a-zA-Z0-9]");
    // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
    private static Pattern stackSnippetBeginPattern = Pattern.compile(".*<!--\\s+begin\\s+snippet[^>]+>");
    private static Pattern stackSnippetEndPattern = Pattern.compile(".*<!--\\s+end\\s+snippet\\s+-->");
    // see https://stackoverflow.com/editing-help#syntax-highlighting
    private static Pattern snippetLanguagePattern = Pattern.compile(".*<!--\\s+language:[^>]+>");

    // database
    private int id;
    private int postId;
    private String userId;
    private byte postHistoryTypeId;
    private String revisionGuid;
    private Timestamp creationDate;
    private String text;
    private String userDisplayName;
    private String comment;
    // internal
    private List<PostBlockVersion> postBlocks;
    private int postTypeId;
    private int localIdCount;

    public static String getRelevantPostHistoryTypes() {
        Integer[] relevantTypes = new Integer[PostHistory.relevantPostHistoryTypes.size()];
        relevantPostHistoryTypes.toArray(relevantTypes);
        StringBuilder relevantTypesString = new StringBuilder();
        for (int i=0; i<relevantTypes.length-1; i++) {
            relevantTypesString.append(relevantTypes[i]).append(",");
        }
        relevantTypesString.append(relevantTypes[relevantTypes.length - 1]);

        return relevantTypesString.toString();
    }

    public PostHistory() {}

    public PostHistory(int id, int postId, int postTypeId, String userId, byte postHistoryTypeId, String revisionGuid,
                       Timestamp creationDate, String text, String userDisplayName, String comment) {
        this.id = id;
        this.postId = postId;
        this.postTypeId = postTypeId;
        this.userId = userId;
        this.postHistoryTypeId = postHistoryTypeId;
        this.revisionGuid = revisionGuid;
        this.creationDate = creationDate;
        this.text = text;
        this.userDisplayName = userDisplayName;
        this.comment = comment;
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
    @Column(name = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Basic
    @Column(name = "PostHistoryTypeId")
    public byte getPostHistoryTypeId() {
        return postHistoryTypeId;
    }

    public void setPostHistoryTypeId(byte postHistoryTypeId) {
        this.postHistoryTypeId = postHistoryTypeId;
    }

    @Basic
    @Column(name = "RevisionGUID")
    public String getRevisionGuid() {
        return revisionGuid;
    }

    public void setRevisionGuid(String revisionGuid) {
        this.revisionGuid = revisionGuid;
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
    @Column(name = "Text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
    @Column(name = "Comment")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Transient
    public int getPostTypeId() {
        return postTypeId;
    }

    public void setPostTypeId(int postTypeId) {
        this.postTypeId = postTypeId;
    }

    @Transient
    public List<PostBlockVersion> getPostBlocks() {
        return postBlocks;
    }

    public void extractPostBlocks() {
        //TODO: Also extract language from HTML comment? (see http://stackoverflow.com/editing-help#syntax-highlighting)

        postBlocks = new LinkedList<>();
        localIdCount = 0;

        // http://stackoverflow.com/a/454913
        String[] lines = text.split("&#xD;&#xA;");
        PostBlockVersion currentBlock = null;

        for (String line : lines) {
            // ignore empty lines
            if (line.isEmpty()) {
                continue;
            }

            // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
            boolean isStackSnippetBegin = stackSnippetBeginPattern.matcher(line).find(); // only match beginning of line
            boolean isStackSnippetEnd = stackSnippetEndPattern.matcher(line).find(); // only match beginning of line

            // ignore Stack Snippet information
            if (isStackSnippetBegin || isStackSnippetEnd) {
                continue;
            }

            // even if tab is not listed here: http://stackoverflow.com/editing-help#code
            // we observed cases where it was important to check for the tab, sometimes preceded by spaces
            // (see test cases)
            boolean isCodeLine = codeBlockPattern.matcher(line).find(); // only match beginning of line
            boolean isWhitespaceLine = whiteSpaceLinePattern.matcher(line).matches(); // match whole line
            boolean containsLettersOrDigits = containsLetterOrDigitPattern.matcher(line).find(); // only match beginning of line
            boolean isSnippetLanguage = snippetLanguagePattern.matcher(line).find(); // only match beginning of line

            if (currentBlock == null) {
                // ignore whitespaces at the beginning of a post
                if (!isWhitespaceLine) {
                    // first line, block element not created yet
                    if (isCodeLine) {
                        currentBlock = new CodeBlockVersion(postId, id);
                    } else {
                        currentBlock = new TextBlockVersion(postId, id);
                    }
                }
            } else {
                // currentBlock has length > 0 => check if current line belongs to this block
                // or if it is first line of next block

                if (currentBlock instanceof TextBlockVersion) {
                    if ((isCodeLine || isSnippetLanguage) && !isWhitespaceLine) {
                        // End of text block, beginning of code block.
                        // Do not end text block if next line is whitespace line
                        // see, e.g., second line of PostHistory, Id=97576027
                        addPostBlock(currentBlock);
                        currentBlock = new CodeBlockVersion(postId, id);
                    }
                }

                if (currentBlock instanceof CodeBlockVersion) {
                    // snippet language divides two code blocks (if first block is not empty)
                    if (isSnippetLanguage) {
                        addPostBlock(currentBlock);
                        currentBlock = new CodeBlockVersion(postId, id);
                    } else if(!isCodeLine && !isWhitespaceLine && containsLettersOrDigits) {
                        // Do not close code postBlocks when whitespace line is reached
                        // see, e.g., PostHistory, Id=55158265, PostId=20991163 (-> test case).
                        // Do not end code block if next line is whitespace line
                        // see, e.g., second line of PostHistory, Id=97576027
                        // Only end code block if next line contains a letter or a digit (e.g., '{').
                        addPostBlock(currentBlock);
                        currentBlock = new TextBlockVersion(postId, id);
                    }
                }
            }

            // ignore snippet language information (see https://stackoverflow.com/editing-help#syntax-highlighting)
            if (currentBlock != null && !isSnippetLanguage) {
                currentBlock.append(line);
            }
        }

        if (currentBlock != null) {
            if (!currentBlock.isEmpty()) {
                // last block not added yet
                currentBlock.setLocalId(++localIdCount);
                postBlocks.add(currentBlock);
            }
        }

        for (PostBlockVersion currentPostBlock : postBlocks) {
            currentPostBlock.finalizeContent();
        }
    }

    private void addPostBlock(PostBlockVersion postBlock) {
        // only add non-empty post blocks
        if (postBlock.getContent().trim().length() > 0) {
            postBlock.setLocalId(++localIdCount);
            postBlocks.add(postBlock);
        }
    }

    public PostVersion toPostVersion() {
        // convert PostHistory (SO Database Schema) to PostVersion (Our Schema)
        PostVersion postVersion = new PostVersion(postId, id, postTypeId);
        postVersion.addPostBlockList(postBlocks);
        return postVersion;
    }

    @Override
    public String toString() {
        String str = "----- PostHistory ------------------" + "\n";
        str += "Id: " + id + "\nCreationDate: "+ creationDate + "\n";
        str += "PostId: "+ postId + "\n";
        str += "----------------------------------------";
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostHistory that = (PostHistory) o;

        if (id != that.getId()) return false;
        if (postId != that.getPostId()) return false;
        if (postTypeId != that.getPostTypeId()) return false;
        if (userId != null ? !userId.equals(that.getUserId()) : that.getUserId() != null) return false;
        if (postHistoryTypeId != that.getPostHistoryTypeId()) return false;
        if (revisionGuid != null ? !revisionGuid.equals(that.getRevisionGuid()) : that.getRevisionGuid() != null) return false;
        if (creationDate != null ? !creationDate.equals(that.getCreationDate()) : that.getCreationDate() != null) return false;
        if (text != null ? !text.equals(that.getText()) : that.getText() != null) return false;
        if (userDisplayName != null ? !userDisplayName.equals(that.getUserDisplayName()) : that.getUserDisplayName() != null)
            return false;
        if (comment != null ? !comment.equals(that.getComment()) : that.getComment() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + postId;
        result = 31 * result + postTypeId;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (int) postHistoryTypeId;
        result = 31 * result + (revisionGuid != null ? revisionGuid.hashCode() : 0);
        result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (userDisplayName != null ? userDisplayName.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        return result;
    }
}
