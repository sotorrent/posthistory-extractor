package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.soposthistory.diffs.LineDiff;
import de.unitrier.st.soposthistory.diffs.diff_match_patch;

import javax.persistence.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.history.PostHistoryIterator.logger;

@Entity
@Table(name = "PostBlockVersion", schema = "stackoverflow16_12")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PostBlockTypeId",discriminatorType=DiscriminatorType.INTEGER)
public abstract class PostBlockVersion {
    /*
     * Decisions:
     *  (1) Only direct predecessors are compared (in snippet 326440, for instance, blocks in version 5 and 8 are
     *      similar, which we ignore).
     *  (2) Merging of blocks is not possible to model in our database layout. In most cases, the larger block will be
     *      more similar than the smaller one and thus be set as a predecessor of the new block. The smaller block will
     *      appear to be deleted.
     *  (3) For the analysis, focus on versions where the code blocks changed. Ignore changes to text blocks.
     */

    // database
    protected int id;
    protected Integer postId;
    protected Integer postHistoryId;
    protected Integer localId;
    protected Integer rootPostBlockVersionId;
    protected String content;
    protected int length;
    protected int lineCount;
    protected Integer predPostBlockId;
    protected Boolean predEqual;
    protected Double predSimilarity;
    protected int predCount;
    protected int succCount;
    // internal
    private StringBuilder contentBuilder;
    private final LineDiff lineDiff;
    private List<diff_match_patch.Diff> predDiff;
    private PostBlockVersion pred;
    private List<diff_match_patch.Diff> succDiff;

    public PostBlockVersion() {
        // database
        this.postId = null;
        this.postHistoryId = null;
        this.localId = null;
        this.rootPostBlockVersionId = null;
        this.content = null;
        this.length = 0;
        this.lineCount = 0;
        this.predPostBlockId = null;
        this.predEqual = null;
        this.predSimilarity = null;
        this.predCount = 0;
        this.succCount = 0;
        // internal
        this.contentBuilder = new StringBuilder();
        this.lineDiff = new LineDiff();
        this.pred = null;
        this.predDiff = null;
        this.succDiff = null;
    }

    public PostBlockVersion(int postId, int postHistoryId) {
        this();
        this.postId = postId;
        this.postHistoryId = postHistoryId;
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
    @Column(name = "PostId")
    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    @Basic
    @Column(name = "LocalId")
    public Integer getLocalId() {
        return localId;
    }

    public void setLocalId(Integer localId) {
        this.localId = localId;
    }

    @Basic
    @Column(name = "RootPostBlockVersionId")
    public Integer getRootPostBlockVersionId() {
        return rootPostBlockVersionId;
    }

    public void setRootPostBlockVersionId(Integer rootPostBlockVersionId) {
        this.rootPostBlockVersionId = rootPostBlockVersionId;
    }

    @Basic
    @Column(name = "Content")
    public String getContent() {
        return content == null ? contentBuilder.toString() : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Basic
    @Column(name = "Length")
    public int getLength() {
        return content == null ? contentBuilder.length() : content.length();
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Basic
    @Column(name = "LineCount")
    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    @Basic
    @Column(name = "PredPostBlockId")
    public Integer getPredPostBlockId() {
        return predPostBlockId;
    }

    public void setPredPostBlockId(Integer predPostBlockId) {
        this.predPostBlockId = predPostBlockId;
    }

    @Basic
    @Column(name = "PredEqual")
    public Boolean getPredEqual() {
        return predEqual;
    }

    public void setPredEqual(Boolean predEqual) {
        this.predEqual = predEqual;
    }

    @Basic
    @Column(name = "PredSimilarity")
    public Double getPredSimilarity() {
        return predSimilarity;
    }

    public void setPredSimilarity(Double predSimilarity) {
        this.predSimilarity = predSimilarity;
    }

    @Basic
    @Column(name = "PredCount")
    public int getPredCount() {
        return predCount;
    }

    public void setPredCount(int predCount) {
        this.predCount = predCount;
    }

    public void incrementPredCount() {
        this.predCount++;
    }

    @Basic
    @Column(name = "SuccCount")
    public int getSuccCount() {
        return succCount;
    }

    public void setSuccCount(int succCount) {
        this.succCount = succCount;
    }

    public void incrementSuccCount() {
        this.succCount++;
    }

    @Transient
    public PostBlockVersion getPred() {
        return pred;
    }

    public void setPred(PostBlockVersion pred, double predSimilarity) {
        try {
            this.pred = pred;
            this.predSimilarity = predSimilarity;
            this.predPostBlockId = pred.getId();
            predDiff = diff(pred);
        }catch(Exception e){
            logger.log(Level.WARNING, "Couldn't set predecessor.");
        }
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

        this.length++;
    }

    public void finalizeContent() {
        this.content = contentBuilder.toString();
        this.contentBuilder = null;
        this.length = content.length();
        this.lineCount = content.split("\\n").length;
    }

    @Transient
    public boolean isEmpty() {
        return content == null ? contentBuilder.length() == 0 : content.length() == 0;
    }

    @Transient
    public List<diff_match_patch.Diff> getPredDiff() {
        return predDiff;
    }

    @Transient
    public List<diff_match_patch.Diff> getSuccDiff() {
        return succDiff;
    }

    protected String composeId() {
            return getPostId() + "-" + getPostHistoryId() + "-" + getLocalId();
    }

    // TODO: after preliminary evaluation: 2-Grams and Dice for code blocks and 4-Grams and Overlap for text blocks
    abstract public double compareTo(PostBlockVersion otherBlock);

    private List<diff_match_patch.Diff> diff(PostBlockVersion block) {
        return lineDiff.diff_lines_only(this.getContent(), block.getContent());
    }

    @Override
    public String toString() {
        return "Changed: " + (predSimilarity == null ? "-" : predSimilarity != 1.0) + "\n"
                + "RootPostBlock: " + rootPostBlockVersionId + "\n"
                + "---Predecessor---\n"
                + "Count: " + (predCount)
                + "Id: " + (pred == null ? "none" : pred.composeId()) + "\n"
                + "Similarity: " + (predSimilarity == null ? "-" : (new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.ENGLISH)))
                    .format(predSimilarity)) + "\n"
                + "Diff:\n" + (predDiff == null ? "none" : predDiff
                    .stream()
                    .map(diff_match_patch.Diff::toString)
                    .collect(Collectors.joining("\n")))
                + "\n"
                + "---Successor---\n"
                + "Count: " + (succCount)
                + "\n";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBlockVersion that = (PostBlockVersion) o;

        if (id != that.getId()) return false;
        if (postHistoryId != null ? !postHistoryId.equals(that.getPostHistoryId()) : that.getPostHistoryId() != null) return false;
        if (localId != null ? !localId.equals(that.getLocalId()) : that.getLocalId() != null) return false;
        if (rootPostBlockVersionId != null ? !rootPostBlockVersionId.equals(that.getRootPostBlockVersionId()) : that.getRootPostBlockVersionId() != null) return false;
        if (content != null ? !content.equals(that.getContent()) : that.getContent() != null) return false;
        if (predPostBlockId != null ? !predPostBlockId.equals(that.getPredPostBlockId()) : that.getPredPostBlockId() != null) return false;
        if (predEqual != null ? !predEqual.equals(that.getPredEqual()) : that.getPredEqual() != null) return false;
        if (predSimilarity != null ? !predSimilarity.equals(that.getPredSimilarity()) : that.getPredSimilarity() != null) return false;
        if (predCount != that.getPredCount()) return false;
        if (succCount != that.getSuccCount()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (postHistoryId != null ? postHistoryId.hashCode() : 0);
        result = 31 * result + (localId != null ? localId.hashCode() : 0);
        result = 31 * result + (rootPostBlockVersionId != null ? rootPostBlockVersionId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (predPostBlockId != null ? predPostBlockId.hashCode() : 0);
        result = 31 * result + (predSimilarity != null ? predSimilarity.hashCode() : 0);
        result = 31 * result + succCount;
        result = 31 * result + predCount;
        return result;
    }
}
