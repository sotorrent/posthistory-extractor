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
import java.util.regex.Matcher;
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
    private static final Pattern codeBlockPattern = Pattern.compile("^( {4}|[ ]*\\t)");
    private static final Pattern whiteSpaceLinePattern = Pattern.compile("^\\s+$");
    private static final Pattern containsLetterOrDigitPattern = Pattern.compile("[a-zA-Z0-9]");
    // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
    private static final Pattern stackSnippetBeginPattern = Pattern.compile(".*<!--\\s+begin\\s+snippet[^>]+>");
    private static final Pattern stackSnippetEndPattern = Pattern.compile(".*<!--\\s+end\\s+snippet\\s+-->");
    // see https://stackoverflow.com/editing-help#syntax-highlighting
    private static final Pattern snippetLanguagePattern = Pattern.compile(".*<!--\\s+language:[^>]+>");
    // see https://meta.stackexchange.com/q/125148; example: https://stackoverflow.com/posts/32342082/revisions
    private static final Pattern alternativeCodeBlockBeginPattern = Pattern.compile("\\s*(```).*");
    private static final Pattern alternativeCodeBlockEndPattern = Pattern.compile(".*(```)\\s*");
    // see, e.g., source of question 19175014 (<pre><code> ... </pre></code> instead of correct indention)
    private static final Pattern codeTagBeginPattern = Pattern.compile("\\s*<pre><code>");
    private static final Pattern codeTagEndPattern = Pattern.compile(".*</pre></code>");
    // see, e.g., source of question 3381751 version 1 (<script type="text/javascript"> ... </script> instead of correct indention)
    private static final Pattern scriptTagBeginPattern = Pattern.compile("\\s*<script[^>]+>");
    private static final Pattern scriptTagEndPattern = Pattern.compile(".*</script>");

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
        postBlocks = new LinkedList<>();
        localIdCount = 0;

        // the text may be null, see, e.g., PostId 2967852, PostHistoryId 6127192
        if (text == null) {
            text = "";
            return;
        }

        // http://stackoverflow.com/a/454913
        String[] lines = text.split("&#xD;&#xA;");
        PostBlockVersion currentPostBlock = null;
        boolean inStackSnippetCodeBlock = false;
        boolean inAlternativeCodeBlock = false;
        boolean inCodeTagCodeBlock = false;
        boolean inScriptTagCodeBlock = false;
        boolean codeBlockEndsWithNextLine = false;

        for (String line : lines) {
            // ignore empty lines
            if (line.isEmpty()) {
                continue;
            }

            // end code block which contained a code tag in the previous line (see below)
            if (codeBlockEndsWithNextLine) {
                inCodeTagCodeBlock = false;
                codeBlockEndsWithNextLine = false;
            }

            // check for indented code blocks (Stack Overflow's standard way)
            // even if tab is not listed here: http://stackoverflow.com/editing-help#code
            // we observed cases where it was important to check for the tab, sometimes preceded by spaces
            // (see test cases)
            boolean isCodeLine = codeBlockPattern.matcher(line).find(); // only match beginning of line
            // check if line only contains whitespaces (ignore whitespaces at the beginning of posts and not end blocks with whitespace lines)
            boolean isWhitespaceLine = whiteSpaceLinePattern.matcher(line).matches(); // match whole line
            // e.g. "<!-- language: lang-js -->" (see https://stackoverflow.com/editing-help#syntax-highlighting)
            boolean isSnippetLanguage = snippetLanguagePattern.matcher(line).find(); // only match beginning of line

            // if line is not part of a regular Stack Overflow code block, try to detect alternative code block styles
            if (!isCodeLine && !isWhitespaceLine && !isSnippetLanguage) {
                // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
                boolean isStackSnippetBegin = stackSnippetBeginPattern.matcher(line).find(); // only match beginning of line
                boolean isStackSnippetEnd = stackSnippetEndPattern.matcher(line).find(); // only match beginning of line

                // ignore Stack Snippet information
                if (isStackSnippetBegin) {
                    inStackSnippetCodeBlock = true;
                    continue;
                }

                if (isStackSnippetEnd) {
                    inStackSnippetCodeBlock = false;
                    continue;
                }

                // code block that is marked by <pre><code> ... </pre></code> instead of correct indention
                boolean isCodeTagBegin = codeTagBeginPattern.matcher(line).find(); // only match beginning of line
                boolean isCodeTagEnd = codeTagEndPattern.matcher(line).find(); // only match beginning of line

                if (isCodeTagBegin) {
                    line = line.replace("<pre><code>", "");
                    inCodeTagCodeBlock = true;
                    if (line.trim().isEmpty()) {
                        // line only contained opening code tags -> skip
                        continue;
                    }
                }

                if (isCodeTagEnd) {
                    line = line.replace("</pre></code>", "");
                    if (line.trim().isEmpty()) {
                        // line only contained closing code tags -> close code block and skip
                        inCodeTagCodeBlock = false;
                        continue;
                    } else {
                        // line also contained content -> close code block in next line
                        codeBlockEndsWithNextLine = true;
                    }
                }

                // code block that is marked by <script...> ... </script> instead of correct indention
                boolean isScriptTagBegin = scriptTagBeginPattern.matcher(line).find(); // only match beginning of line
                boolean isScriptTagEnd = scriptTagEndPattern.matcher(line).find(); // only match beginning of line

                if (isScriptTagBegin) {
                    line = line.replaceFirst("<script[^>]+>", "");
                    inScriptTagCodeBlock = true;
                    if (line.trim().isEmpty()) {
                        // line only contained opening script tag -> skip
                        continue;
                    }
                }

                if (isScriptTagEnd) {
                    line = line.replace("</script>", "");
                    if (line.trim().isEmpty()) {
                        // line only contained closing script tag -> close code block and skip
                        inScriptTagCodeBlock = false;
                        continue;
                    } else {
                        // line also contained content -> close script block in next line
                        codeBlockEndsWithNextLine = true;
                    }
                }

                // see https://meta.stackexchange.com/q/125148; example: https://stackoverflow.com/posts/32342082/revisions
                Matcher alternativeCodeBlockBeginMatcher = alternativeCodeBlockBeginPattern.matcher(line);
                boolean isAlternativeCodeBlockBegin = alternativeCodeBlockBeginMatcher.matches(); // match whole line
                Matcher alternativeCodeBlockEndMatcher = alternativeCodeBlockEndPattern.matcher(line);
                boolean isAlternativeCodeBlockEnd = alternativeCodeBlockEndMatcher.matches(); // match whole line

                if (isAlternativeCodeBlockBegin) {
                    // remove "```" from line
                    line = alternativeCodeBlockBeginMatcher.replaceAll("");
                    inAlternativeCodeBlock = true;
                    // continue if line only contained "```"
                    if (line.trim().length() == 0) {
                        continue;
                    }
                }

                if (isAlternativeCodeBlockEnd) {
                    // remove "```" from line
                    line = alternativeCodeBlockEndMatcher.replaceAll("");
                    inAlternativeCodeBlock = false;
                }
            }

            // decide if the current line is part of a code block
            boolean inCodeBlock = isCodeLine || isSnippetLanguage || inStackSnippetCodeBlock || inAlternativeCodeBlock
                    || inCodeTagCodeBlock || inScriptTagCodeBlock;

            if (currentPostBlock == null) {
                // first block in post

                // ignore whitespaces at the beginning of a post
                if (!isWhitespaceLine) {
                    // first line, block element not created yet
                    if (inCodeBlock) {
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    } else {
                        currentPostBlock = new TextBlockVersion(postId, id);
                    }
                }
            } else {
                // currentBlock has length > 0 => check if current line belongs to this block
                // or if it is first line of next block

                if (currentPostBlock instanceof TextBlockVersion) {
                    if (inCodeBlock && !isWhitespaceLine) {
                        // End of text block, beginning of code block.
                        // Do not end text block if next line is whitespace line
                        // see, e.g., second line of PostHistory, Id=97576027
                        addPostBlock(currentPostBlock);
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    }
                }

                if (currentPostBlock instanceof CodeBlockVersion) {
                    // snippet language divides two code blocks (if first block is not empty)
                    if (isSnippetLanguage) {
                        addPostBlock(currentPostBlock);
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    } else if(!inCodeBlock && !isWhitespaceLine) {
                        // In a Stack Snippet, the lines do not have to be indented (see version 12 of answer
                        // 26044128 and corresponding test case).
                        // Do not close code postBlocks when whitespace line is reached
                        // see, e.g., PostHistory, Id=55158265, PostId=20991163 (-> test case).
                        // Do not end code block if next line is whitespace line
                        // see, e.g., second line of PostHistory, Id=97576027
                        addPostBlock(currentPostBlock);
                        currentPostBlock = new TextBlockVersion(postId, id);
                    }
                }
            }

            // ignore snippet language information (see https://stackoverflow.com/editing-help#syntax-highlighting)
            if (currentPostBlock != null && !isSnippetLanguage) {
                currentPostBlock.append(line);
            }
        }

        if (currentPostBlock != null) {
            if (!currentPostBlock.isEmpty()) {
                // last block not added yet
                currentPostBlock.setLocalId(++localIdCount);
                postBlocks.add(currentPostBlock);
            }
        }

        reviseAndFinalizePostBlocks();
    }

    private void reviseAndFinalizePostBlocks() {
        PostBlockVersion currentPostBlock;
        Set<PostBlockVersion> markedForDeletion = new HashSet<>();

        for (int i = 0; i < postBlocks.size(); i++) {
            currentPostBlock = postBlocks.get(i);

            // ignore post block if it is already marked for deletion
            if (markedForDeletion.contains(currentPostBlock)) {
                continue;
            }

            currentPostBlock.finalizeContent();

            /* In some cases when a code blocks ends with a single character, the indention by 4 spaces is missing in
             * the table PostHistory (see, e.g., PostHistoryId=96888165). The following code should prevent most of
             * these cases from being recognized as text blocks.
             */

            // remove this post block if does not contain letters or digits
            boolean containsLettersOrDigits = containsLetterOrDigitPattern.matcher(
                    currentPostBlock.getContent()).find(); // only match beginning of line

            if (containsLettersOrDigits) {
                continue;
            }

            if (i == 0) {
                // current post block is first one
                if (postBlocks.size() > 1) {
                    PostBlockVersion nextPostBlock = postBlocks.get(i+1);
                    nextPostBlock.append(currentPostBlock.getContent());
                    nextPostBlock.finalizeContent();
                    markedForDeletion.add(currentPostBlock);
                }
            } else {
                // current post block is not first one (has predecessor)
                PostBlockVersion previousPostBlock = postBlocks.get(i-1);
                previousPostBlock.append(currentPostBlock.getContent());
                previousPostBlock.finalizeContent();
                markedForDeletion.add(currentPostBlock);

                // current post block must have successor
                if (i >= postBlocks.size()-1) {
                    continue;
                }

                PostBlockVersion nextPostBlock = postBlocks.get(i+1);
                nextPostBlock.finalizeContent();

                // merge predecessor and successor if they have same type
                if (previousPostBlock.getClass() != nextPostBlock.getClass()) {
                    continue;
                }
                previousPostBlock.append(nextPostBlock.getContent());
                previousPostBlock.finalizeContent();
                markedForDeletion.add(nextPostBlock);
            }
        }

        // remove post blocks marked for deletion
        postBlocks.removeAll(markedForDeletion);

        // update local ids after merging of post blocks
        for (int i=0; i<postBlocks.size(); i++) {
            postBlocks.get(i).setLocalId(i+1);
        }
    }

    private void addPostBlock(PostBlockVersion postBlock) {
        // only add non-empty post blocks
        if (postBlock.getContent().trim().length() > 0) {
            postBlock.setLocalId(++localIdCount);
            postBlocks.add(postBlock);
        }
    }

    @Transient
    public PostVersion toPostVersion() {
        // convert PostHistory (SO Database Schema) to PostVersion (Our Schema)
        PostVersion postVersion = new PostVersion(postId, id, postTypeId);
        postVersion.addPostBlockList(postBlocks);
        return postVersion;
    }

    @Override
    public String toString() {
        return "PostHistory: Id=" + id + ", PostId="+ postId;
    }
}
