package de.unitrier.st.soposthistory.blocks;

import de.unitrier.st.soposthistory.diffs.LineDiff;
import de.unitrier.st.soposthistory.diffs.diff_match_patch;
import de.unitrier.st.soposthistory.version.PostVersion;

import javax.persistence.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static de.unitrier.st.soposthistory.history.PostHistoryIterator.logger;

@Entity
@Table(name = "PostBlockVersion", schema = "stackoverflow16_12")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PostBlockTypeId",discriminatorType=DiscriminatorType.INTEGER)
public abstract class PostBlockVersion {
    private static final double EQUALITY_SIMILARITY = 10.0;

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
    protected Integer postVersionId;
    protected Integer localId;
    protected Integer postId;
    protected Integer postHistoryId;
    protected String content;
    protected int length;
    protected int lineCount;
    protected Integer rootPostBlockId;
    protected Integer predPostBlockId;
    protected Boolean predEqual;
    protected Double predSimilarity;
    protected int predCount; // this marks possible predecessors, which may not be available for linking (see below)
    protected int succCount;
    // internal
    private StringBuilder contentBuilder;
    private final LineDiff lineDiff;
    private List<diff_match_patch.Diff> predDiff;
    private PostBlockVersion pred;
    private boolean isAvailable; // false if this post block is set as a predecessor of a block in the next version
    private List<PostBlockVersion> matchingPredecessors;
    private Map<PostBlockVersion, Double> predecessorSimilarities;
    private double maxSimilarity;

    public PostBlockVersion() {
        // database
        this.postVersionId = null;
        this.localId = null;
        this.postId = null;
        this.postHistoryId = null;
        this.content = null;
        this.length = 0;
        this.lineCount = 0;
        this.rootPostBlockId = null;
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
        this.isAvailable = true;
        this.matchingPredecessors = new LinkedList<>();
        this.predecessorSimilarities = new HashMap<>();
        this.maxSimilarity = -1;
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

    public Integer getPostVersionId() {
        return postVersionId;
    }

    @Basic
    @Column(name = "PostVersionId")
    public void setPostVersionId(Integer postVersionId) {
        this.postVersionId = postVersionId;
    }

    @Basic
    @Column(name = "LocalId")
    public Integer getLocalId() {
        return localId;
    }

    public void setLocalId(Integer localId) {
        this.localId = localId;
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
    @Column(name = "RootPostBlockId")
    public Integer getRootPostBlockId() {
        return rootPostBlockId;
    }

    public void setRootPostBlockId(Integer rootPostBlockId) {
        this.rootPostBlockId = rootPostBlockId;
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

    public void setPred(PostBlockVersion pred) {
        if (maxSimilarity == EQUALITY_SIMILARITY) {
            // pred is equal
            setPred(pred, 1.0); // computes diff
            setPredEqual(true);
        } else {
            // pred is similar
            setPred(pred, maxSimilarity); // computes diff
            setPredEqual(false);
        }

        // mark predecessor as not available
        pred.setNotAvailable();
    }

    public boolean setPredMinPos() {
        // set matching predecessor that has minimal position and is still available
        int pos = 0;
        while(pos < matchingPredecessors.size()
                && !matchingPredecessors.get(pos).isAvailable()) {
            pos++;
        }

        if (pos < matchingPredecessors.size()) {
            PostBlockVersion matchingPredecessor = matchingPredecessors.get(pos);
            if (matchingPredecessor.isAvailable()) {
                setPred(matchingPredecessor);
                return true;
            }
        }

        return false;
    }

    public boolean setPredContext(PostVersion currentVersion, PostVersion previousVersion) {
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            if (setPredContext(matchingPredecessor, currentVersion, previousVersion)) {
                return true;
            }
        }
        return false;
    }

    public boolean setPredContext(PostBlockVersion matchingPredecessor, PostVersion currentVersion, PostVersion previousVersion) {
        int indexThis = currentVersion.getPostBlocks().indexOf(this);
        int indexPred = previousVersion.getPostBlocks().indexOf(matchingPredecessor);

        // consider context to select matching predecessor
        if ((indexThis > 0 && indexThis < currentVersion.getPostBlocks().size()-1)
                && (indexPred > 0 && indexPred < previousVersion.getPostBlocks().size()-1)) {

            // neighbors of post block and matching predecessor are available

            // get post blocks before and after this post block
            PostBlockVersion beforePostBlock = currentVersion.getPostBlocks().get(indexThis - 1);
            PostBlockVersion afterPostBlock = currentVersion.getPostBlocks().get(indexThis + 1);

            // get post blocks before and after matching predecessor
            PostBlockVersion beforeMatchingPredecessor = previousVersion.getPostBlocks().get(indexPred - 1);
            PostBlockVersion afterMatchingPredecessor = previousVersion.getPostBlocks().get(indexPred + 1);

            // use different strategies for code and text blocks
            if (this instanceof CodeBlockVersion) {
                // check if matching predecessor has same neighbors
                if (beforePostBlock.getPred() != null && beforePostBlock.getPred() == beforeMatchingPredecessor
                        && afterPostBlock.getPred() != null && afterPostBlock.getPred() == afterMatchingPredecessor) {
                    if (matchingPredecessor.isAvailable()) {
                        setPred(matchingPredecessor);
                        return true;
                    }
                }
            } else if (this instanceof TextBlockVersion) {
                // consider text as caption for next code block -> focus on afterPostBlock (beforePostBlock may be null)
                if ((beforePostBlock.getPred() == null ||
                        (beforePostBlock.getPred() != null && beforePostBlock.getPred() == beforeMatchingPredecessor))
                        && afterPostBlock.getPred() != null && afterPostBlock.getPred() == afterMatchingPredecessor) {
                    if (matchingPredecessor.isAvailable()) {
                        setPred(matchingPredecessor);
                        return true;
                    }
                }
            }
        }

        return false;
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
    public boolean isAvailable() {
        return isAvailable;
    }

    public void setNotAvailable() {
        if (!isAvailable) {
            throw new IllegalStateException("A post block can only be prececessor of one post block in the next verion.");
        } else {
            isAvailable = false;
        }
    }

    abstract public double compareTo(PostBlockVersion otherBlock);

    private List<diff_match_patch.Diff> diff(PostBlockVersion block) {
        return lineDiff.diff_lines_only(this.getContent(), block.getContent());
    }

    @Transient
    public List<PostBlockVersion> getMatchingPredecessors() {
        return matchingPredecessors;
    }

    abstract public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks);

    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks,
                                                                      double similarityThreshold) {

        for (PostBlockVersion previousVersionPostBlock : previousVersionPostBlocks) {
            boolean equal = getContent().equals(previousVersionPostBlock.getContent());
            double similarity = compareTo(previousVersionPostBlock);

            if (equal) {
                // equal predecessors have similarity 10.0 (see final static constant EQUALITY_SIMILARITY)
                predecessorSimilarities.put(previousVersionPostBlock, EQUALITY_SIMILARITY);
                maxSimilarity = EQUALITY_SIMILARITY;
            } else {
                predecessorSimilarities.put(previousVersionPostBlock, similarity);
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity;
                }
            }
        }

        // set most similar post block as predecessor:
        // (1) equality of content, (2) similarity metric, (3) context, (4) order in post.
        final double finalMaxSimilarity = maxSimilarity; // final value needed for lambda expression

        if (finalMaxSimilarity >= similarityThreshold) {
            // get predecessors with max. similarity
            matchingPredecessors = predecessorSimilarities.entrySet()
                    .stream()
                    .filter(e -> e.getValue() == finalMaxSimilarity)
                    .sorted(Comparator.comparing(e -> e.getKey().getLocalId())) // TODO: asc or desc???
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // increase successor count for all matching predecessors
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            incrementPredCount();
            matchingPredecessor.incrementSuccCount();
        }

        return matchingPredecessors;
    }

    @Override
    public String toString() {
        return "PostBlockVersion: " + getContent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBlockVersion that = (PostBlockVersion) o;

        if (id != that.getId()) return false;
        if (postHistoryId != null ? !postHistoryId.equals(that.getPostHistoryId()) : that.getPostHistoryId() != null) return false;
        if (localId != null ? !localId.equals(that.getLocalId()) : that.getLocalId() != null) return false;
        if (rootPostBlockId != null ? !rootPostBlockId.equals(that.getRootPostBlockId()) : that.getRootPostBlockId() != null) return false;
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
        result = 31 * result + (rootPostBlockId != null ? rootPostBlockId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (predPostBlockId != null ? predPostBlockId.hashCode() : 0);
        result = 31 * result + (predSimilarity != null ? predSimilarity.hashCode() : 0);
        result = 31 * result + succCount;
        result = 31 * result + predCount;
        return result;
    }
}
