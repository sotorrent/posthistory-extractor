package org.sotorrent.posthistoryextractor.history;

import org.sotorrent.posthistoryextractor.blocks.CodeBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.PostBlockVersion;
import org.sotorrent.posthistoryextractor.blocks.TextBlockVersion;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.TitleVersion;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.sotorrent.util.FileUtils;
import org.sotorrent.util.LogUtils;

import javax.persistence.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name="PostHistory")
public class PostHistory {
    private static Logger logger = null;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(PostHistory.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    public static final Set<Byte> contentPostHistoryTypes = new HashSet<>();
    static {
        // see https://meta.stackexchange.com/a/2678
        contentPostHistoryTypes.add((byte)2); // Initial Body - initial post raw body text
        contentPostHistoryTypes.add((byte)5); // Edit Body - modified post body (raw markdown)
        contentPostHistoryTypes.add((byte)8); // Rollback Body - reverted body (raw markdown)
    }

    public static final Set<Byte> titlePostHistoryTypes = new HashSet<>();
    static {
        // see https://meta.stackexchange.com/a/2678
        titlePostHistoryTypes.add((byte)1); // Initial Title - initial title (questions only)
        titlePostHistoryTypes.add((byte)4); // Edit Title - modified title (questions only)
        titlePostHistoryTypes.add((byte)7); // Rollback Title - reverted title (questions only)
    }

    public static final Pattern fileNamePattern = Pattern.compile("(\\d+)\\.csv");

    public static final CSVFormat csvFormatPostHistory;
    static {
        // configure CSV format for ground truth
        csvFormatPostHistory = CSVFormat.DEFAULT
                .withHeader("Id", "PostId", "UserId", "PostHistoryTypeId", "RevisionGUID", "CreationDate", "Text", "UserDisplayName", "Comment")
                .withDelimiter(';')
                .withQuote('"')
                .withQuoteMode(QuoteMode.MINIMAL)
                .withEscape('\\')
                .withFirstRecordAsHeader();
    }

    // regex for escaped newline characters
    public static final String newLineRegex = "((?:&#xD;|&#xA;)?&#xA;)";

    // a code block is indented by four spaces or a tab (which can be preceded by spaces)
    private static final Pattern codeBlockPattern = Pattern.compile("^( {4}|[ ]*\\t)");
    private static final Pattern whiteSpaceLinePattern = Pattern.compile("^\\s+$");
    private static final Pattern containsLetterOrDigitPattern = Pattern.compile("[a-zA-Z0-9]");
    // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
    private static final Pattern stackSnippetBeginPattern = Pattern.compile("<!--\\s+begin\\s+snippet[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern stackSnippetEndPattern = Pattern.compile("<!--\\s+end\\s+snippet\\s+-->", Pattern.CASE_INSENSITIVE);
    private static final Pattern snippetDividerPattern = Pattern.compile("<([!?])--\\s+-->\\s*");
    // see https://stackoverflow.com/editing-help#syntax-highlighting
    private static final Pattern snippetLanguagePattern = Pattern.compile("<!--\\s+language:[^>]+>", Pattern.CASE_INSENSITIVE);
    // see https://meta.stackexchange.com/q/125148; example: https://stackoverflow.com/posts/32342082/revisions
    private static final Pattern alternativeCodeBlockBeginPattern = Pattern.compile("^\\s*(```)");
    private static final Pattern alternativeCodeBlockEndPattern = Pattern.compile("(```)\\s*$");
    // see, e.g., source of question 19175014 (<pre><code> ... </pre></code> instead of correct indention)
    private static final Pattern codeTagBeginPattern = Pattern.compile("^\\s*(<pre[^>]*>)|(<pre[^>]*>\\s*<code>)|(<code>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern codeTagEndPattern = Pattern.compile("(</code>)|(</code>\\s*</pre>)|(</pre>)\\s*$", Pattern.CASE_INSENSITIVE);
    // see, e.g., source of question 3381751 version 1 (<script type="text/javascript"> ... </script> instead of correct indention)
    private static final Pattern scriptTagBeginPattern = Pattern.compile("^\\s*<script[^>]+>", Pattern.CASE_INSENSITIVE);
    private static final Pattern scriptTagEndPattern = Pattern.compile("</script>\\s*$", Pattern.CASE_INSENSITIVE);
    // see, e.g., source of question 17158055, version 6 (line containing only `mydomain.com/bn/products/1`)
    // also consider possible HTML newline character, as in question 49311849, version 4
    private static final Pattern inlineCodeLinePattern = Pattern.compile("\\s*`([^`]+)`\\s*(?:<br\\s*/?\\s*>)?", Pattern.CASE_INSENSITIVE);

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
    private int localIdCount;

    public PostHistory() {}

    public PostHistory(int id, int postId, String userId, byte postHistoryTypeId, String revisionGuid,
                       Timestamp creationDate, String text, String userDisplayName, String comment) {
        this.id = id;
        this.postId = postId;
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
        String[] lines = text.split(newLineRegex);
        PostBlockVersion currentPostBlock = null;
        boolean inStackSnippetCodeBlock = false;
        boolean inAlternativeCodeBlock = false;
        boolean inCodeTagCodeBlock = false;
        boolean inScriptTagCodeBlock = false;
        boolean codeBlockEndsWithNextLine = false;
        String previousLine = "";

        for (String line : lines) {
            // ignore empty lines
            if (line.isEmpty()) {
                previousLine = line;
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
            boolean inMarkdownCodeBlock = codeBlockPattern.matcher(line).find(); // only match beginning of line
            // check if line only contains whitespaces (ignore whitespaces at the beginning of posts and not end blocks with whitespace lines)
            boolean isWhitespaceLine = whiteSpaceLinePattern.matcher(line).matches(); // match whole line
            // e.g. "<!-- language: lang-js -->" (see https://stackoverflow.com/editing-help#syntax-highlighting)
            Matcher snippetLanguageMatcher = snippetLanguagePattern.matcher(line);
            boolean isSnippetLanguage = snippetLanguageMatcher.matches(); // match whole line
            // in some posts an empty XML comment ("<!-- -->") is used to divide code blocks (see, e.g., post 33058542)
            boolean isSnippetDivider = snippetDividerPattern.matcher(line).matches();
            // in some cases, there are inline code blocks in a single line (`...`)
            Matcher inlineCodeLineMatcher = inlineCodeLinePattern.matcher(line);
            boolean isInlineCodeLine = inlineCodeLineMatcher.matches();

            // if line is not part of a regular Stack Overflow code block, try to detect alternative code block styles
            if (!inMarkdownCodeBlock && !isWhitespaceLine && !isSnippetLanguage) {

                // see https://stackoverflow.blog/2014/09/16/introducing-runnable-javascript-css-and-html-code-snippets/
                Matcher stackSnippetBeginMatcher = stackSnippetBeginPattern.matcher(line);
                boolean isStackSnippetBegin = stackSnippetBeginMatcher.find(); // only match beginning of line
                // ignore stack snippet begin
                if (isStackSnippetBegin) {
                    inStackSnippetCodeBlock = true;
                    line = stackSnippetBeginMatcher.replaceAll("");
                    if (line.trim().isEmpty()) {
                        // line only contained stack snippet begin
                        continue;
                    }
                }

                Matcher stackSnippetEndMatcher = stackSnippetEndPattern.matcher(line);
                boolean isStackSnippetEnd = stackSnippetEndMatcher.find(); // only match beginning of line
                // ignore stack snippet end
                if (isStackSnippetEnd) {
                    inStackSnippetCodeBlock = false;
                    line = stackSnippetEndMatcher.replaceAll("");
                    if (line.trim().isEmpty()) {
                        // line only contained stack snippet begin
                        continue;
                    }
                }

                // code block that is marked by <pre><code> ... </pre></code> instead of correct indention
                boolean isCodeTagBegin = codeTagBeginPattern.matcher(line).find(); // only match beginning of line
                if (isCodeTagBegin) {
                    line = line.replaceAll(codeTagBeginPattern.pattern(), "");
                    inCodeTagCodeBlock = true;
                    if (line.trim().isEmpty()) {
                        // line only contained opening code tags -> skip
                        continue;
                    }
                }

                boolean isCodeTagEnd = codeTagEndPattern.matcher(line).find(); // only match beginning of line
                if (isCodeTagEnd) {
                    line = line.replaceAll(codeTagEndPattern.pattern(), "");
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
                if (isScriptTagBegin) {
                    line = line.replaceFirst("<script[^>]+>", "");
                    inScriptTagCodeBlock = true;
                    if (line.trim().isEmpty()) {
                        // line only contained opening script tag -> skip
                        continue;
                    }
                }

                boolean isScriptTagEnd = scriptTagEndPattern.matcher(line).find(); // only match beginning of line
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
                boolean isAlternativeCodeBlockBegin = alternativeCodeBlockBeginMatcher.find(); // only match beginning of line
                if (isAlternativeCodeBlockBegin) {
                    // remove first "```" from line
                    line = alternativeCodeBlockBeginMatcher.replaceFirst("");
                    inAlternativeCodeBlock = true;
                    // continue if line only contained "```"
                    if (line.trim().length() == 0) {
                        continue;
                    } else {
                        if (line.contains("```")) {
                            // alternative code block was inline code block (which should be part of a text block)
                            line = line.replaceAll("```", "");
                            inAlternativeCodeBlock = false;
                        }
                    }
                }

                Matcher alternativeCodeBlockEndMatcher = alternativeCodeBlockEndPattern.matcher(line);
                boolean isAlternativeCodeBlockEnd = alternativeCodeBlockEndMatcher.find(); // only match beginning of line
                if (isAlternativeCodeBlockEnd) {
                    // remove "```" from line
                    line = alternativeCodeBlockEndMatcher.replaceAll("");
                    inAlternativeCodeBlock = false;
                }
            }

            if (isSnippetLanguage) {
                // remove snippet language information
                line = snippetLanguageMatcher.replaceAll("");
            }

            if (isInlineCodeLine) {
                // replace leading and trailing backtick and HTML line break if present
                line = inlineCodeLineMatcher.group(1);
            }

            // decide if the current line is part of a code block
            boolean inNonMarkdownCodeBlock = (isSnippetLanguage && line.trim().length() == 0) || inStackSnippetCodeBlock
                    || inAlternativeCodeBlock || inCodeTagCodeBlock || inScriptTagCodeBlock || isInlineCodeLine;

            if (currentPostBlock == null) {
                // first block in post

                // ignore whitespaces at the beginning of a post
                if (!isWhitespaceLine) {
                    // first line, block element not created yet
                    if (inMarkdownCodeBlock || inNonMarkdownCodeBlock) {
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    } else {
                        currentPostBlock = new TextBlockVersion(postId, id);
                    }
                }
            } else {
                // currentBlock has length > 0 => check if current line belongs to this block
                // or if it is first line of next block

                if (currentPostBlock instanceof TextBlockVersion) {
                    // check if line contains letters or digits (heuristic for malformed post blocks)
                    boolean previousLineContainsLettersOrDigits = containsLetterOrDigitPattern.matcher(previousLine).find();

                    if (((inMarkdownCodeBlock && (previousLine.isEmpty() || !previousLineContainsLettersOrDigits))
                            || inNonMarkdownCodeBlock) && !isWhitespaceLine) {
                        // End of text block, beginning of code block.
                        // Do not end text block if next line is whitespace line
                        // see, e.g., second line of PostHistory, Id=97576027
                        addPostBlock(currentPostBlock);
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    }
                }

                if (currentPostBlock instanceof CodeBlockVersion) {
                    // snippet language or snippet divider divide two code blocks (if first block is not empty)
                    if (isSnippetLanguage || isSnippetDivider) {
                        addPostBlock(currentPostBlock);
                        currentPostBlock = new CodeBlockVersion(postId, id);
                    } else if ((!inMarkdownCodeBlock && !inNonMarkdownCodeBlock) && !isWhitespaceLine) {
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

            previousLine = line;
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
                    nextPostBlock.prepend(currentPostBlock.getContent());
                    nextPostBlock.finalizeContent();
                    markedForDeletion.add(currentPostBlock);
                }
            } else {
                // current post block is not first one (has predecessor)
                PostBlockVersion previousPostBlock = postBlocks.get(i-1);

                if (markedForDeletion.contains(previousPostBlock)) {
                    continue;
                }

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
    public PostVersion toPostVersion(byte postTypeId) {
        if (!contentPostHistoryTypes.contains(postHistoryTypeId)) {
            throw new IllegalStateException("Only versions modifying the content can be exported to PostVersion.");
        }

        // convert PostHistory (SO database schema) to PostVersion (our schema)
        PostVersion postVersion = new PostVersion(postId, postTypeId, id, postHistoryTypeId, creationDate);
        postVersion.addPostBlockList(postBlocks);
        return postVersion;
    }

    @Transient
    public TitleVersion toTitleVersion(byte postTypeId) {
        if (!titlePostHistoryTypes.contains(postHistoryTypeId)) {
            throw new IllegalStateException("Only versions modifying the title can be exported to TitleVersion.");
        }

        // convert PostHistory (SO database schema) to TitleVersion (our schema)
        return new TitleVersion(postId, id, postTypeId, postHistoryTypeId, creationDate, text);
    }

    public static List<PostHistory> readFromCSV(Path dir, int postId, Set<Byte> postHistoryTypes) {
        // ensure that input directory exists
        try {
            FileUtils.ensureDirectoryExists(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<PostHistory> postHistoryList = new LinkedList<>();
        Path pathToCSVFile = Paths.get(dir.toString(), postId + ".csv");

        CSVParser parser;
        try {
            parser = CSVParser.parse(
                    pathToCSVFile.toFile(),
                    StandardCharsets.UTF_8,
                    PostHistory.csvFormatPostHistory
            );
            parser.getHeaderMap();

            logger.info("Reading version data from CSV file " + pathToCSVFile.toFile().toString() + " ...");

            List<CSVRecord> records = parser.getRecords();

            if (records.size() > 0) {
                for (CSVRecord record : records) {
                    byte postHistoryTypeId = Byte.parseByte(record.get("PostHistoryTypeId"));
                    // only consider relevant post history types
                    if (!postHistoryTypes.contains(postHistoryTypeId)) {
                        continue;
                    }

                    String text = record.get("Text");
                    // ignore entries where column "Text" is empty
                    if (text.isEmpty()) {
                        continue;
                    }

                    int id = Integer.parseInt(record.get("Id"));
                    assert postId == Integer.parseInt(record.get("PostId"));
                    String userId = record.get("UserId");
                    String revisionGuid = record.get("RevisionGUID");
                    Timestamp creationDate = Timestamp.valueOf(record.get("CreationDate"));
                    String userDisplayName = record.get("UserDisplayName");
                    String comment = record.get("Comment");

                    PostHistory postHistory = new PostHistory(
                            id, postId, userId, postHistoryTypeId, revisionGuid, creationDate,
                            text, userDisplayName, comment);

                    postHistoryList.add(postHistory);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return postHistoryList;
    }

    @Override
    public String toString() {
        return "PostHistory: Id=" + id + ", PostId="+ postId;
    }
}
