package de.unitrier.st.soposthistory.blocks;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.diffs.LineDiff;
import de.unitrier.st.soposthistory.diffs.diff_match_patch;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.stringsimilarity.util.IllegalSimilarityValueException;
import de.unitrier.st.stringsimilarity.util.InputTooShortException;
import de.unitrier.st.util.Util;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Entity
@Table(name = "PostBlockVersion", schema = "stackoverflow16_12")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PostBlockTypeId",discriminatorType=DiscriminatorType.INTEGER)
public abstract class PostBlockVersion {
    public static final double EQUALITY_SIMILARITY = 10.0;
    private static Logger logger = null;

    static {
        // configure logger
        try {
            logger = Util.getClassLogger(PostBlockVersion.class, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Decisions:
     *  (1) Only direct predecessors are compared (in snippet 326440, for instance, blocks in version 5 and 8 are
     *      similar, which we ignore).
     *  (2) Merging of blocks is not possible to model in our database layout. In most cases, the larger block will be
     *      more similar than the smaller one and thus be set as a predecessor of the new block. The smaller block will
     *      appear as if it has been deleted.
     *  (3) For the analysis, we focus on versions where the code blocks changed.
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
    private LineDiff lineDiff;
    private List<diff_match_patch.Diff> predDiff;
    private PostBlockVersion pred;
    private PostBlockVersion succ;
    private PostBlockVersion rootPostBlock;
    private boolean isAvailable; // false if this post block is set as a predecessor of a block in the next version
    private List<PostBlockVersion> matchingPredecessors;
    private Map<PostBlockVersion, Double> predecessorSimilarities;
    private double maxSimilarity;
    private double similarityThreshold;
    private boolean lifeSpanExtracted; // for extraction of PostBlockLifeSpan
    private Set<PostBlockVersion> failedPredecessorsComparisons; // needed for metrics comparison

    public PostBlockVersion() {
        // database
        this.postVersionId = null;
        this.localId = null;
        this.postId = null;
        this.postHistoryId = null;
        this.content = null;
        this.length = 0;
        this.lineCount = 0;
        this.content = null;
        this.length = 0;
        this.lineCount = 0;
        // internal
        this.contentBuilder = new StringBuilder();
        this.lineDiff = new LineDiff();
        // database + internal
        this.resetVersionHistory();
    }

    public PostBlockVersion(int postId, int postHistoryId) {
        this();
        this.postId = postId;
        this.postHistoryId = postHistoryId;
    }

    // reset data set in PostVersionList.processVersionHistory (needed for metrics comparison)
    public void resetVersionHistory() {
        // database
        this.rootPostBlockId = null;
        this.predPostBlockId = null;
        this.predEqual = null;
        this.predSimilarity = null;
        this.predCount = 0;
        this.succCount = 0;
        // internal
        this.pred = null;
        this.succ = null;
        this.rootPostBlock = null;
        this.predDiff = null;
        this.isAvailable = true;
        this.matchingPredecessors = new LinkedList<>();
        this.predecessorSimilarities = new HashMap<>();
        this.maxSimilarity = -1.0;
        this.similarityThreshold = -1.0;
        this.lifeSpanExtracted = false;
        this.failedPredecessorsComparisons = new HashSet<>();
    }

    @Id
    @Column(name = "Id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        } catch (Exception e) {
            logger.warning("Unable to set predecessor " + pred);
            e.printStackTrace();
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

    public void setPredMinPos() {
        // set matching predecessor that has minimal position and is still available

        // find available matching predecessor
        int pos = 0;
        while (pos < matchingPredecessors.size() && !matchingPredecessors.get(pos).isAvailable()) {
            pos++;
        }

        if (pos < matchingPredecessors.size()) {
            // available matching predecessor found --> set as predecessor of this PostBlockVersion
            PostBlockVersion matchingPredecessor = matchingPredecessors.get(pos);
            setPred(matchingPredecessor);
            matchingPredecessor.setSucc(this);
        }
    }

    public void setPredContext(PostVersion currentVersion, PostVersion previousVersion) {
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            setPredContext(matchingPredecessor, currentVersion, previousVersion);
        }
    }

    public void setPredContext(PostBlockVersion matchingPredecessor, PostVersion currentVersion, PostVersion previousVersion) {
        // consider context to select matching predecessor

        // if the matching predecessor is not available, it cannot be set
        if (!matchingPredecessor.isAvailable()) {
            return;
        }

        // retrieve context of this post block and the matching predecessor
        int indexThis = currentVersion.getPostBlocks().indexOf(this);
        int indexPred = previousVersion.getPostBlocks().indexOf(matchingPredecessor);
        boolean neighborsAvailableThis = indexThis > 0 && indexThis < currentVersion.getPostBlocks().size() - 1;
        boolean neighborsAvailablePred = indexPred > 0 && indexPred < previousVersion.getPostBlocks().size() - 1;

        if (!neighborsAvailableThis || !neighborsAvailablePred) {
            // neighbors of this post block and the matching predecessor are not available
            return;
        }

        // get post blocks before and after this post block
        PostBlockVersion beforeThis = currentVersion.getPostBlocks().get(indexThis - 1);
        PostBlockVersion afterThis = currentVersion.getPostBlocks().get(indexThis + 1);

        // get post blocks before and after matching predecessor
        PostBlockVersion beforePred = previousVersion.getPostBlocks().get(indexPred - 1);
        PostBlockVersion afterPred = previousVersion.getPostBlocks().get(indexPred + 1);

        // check if matching predecessor has same neighbors
        boolean beforeMatch = beforeThis.getPred() != null
                && beforeThis.getPred().getContent().trim().equals(beforePred.getContent().trim());
        boolean afterMatch = afterThis.getPred() != null
                && afterThis.getPred().getContent().trim().equals(afterPred.getContent().trim());

        // use different strategies for code and text blocks
        if (this instanceof CodeBlockVersion) {
            // consider both the post blocks before and after the current post block and the matching predecessor
            if (beforeMatch && afterMatch) {
                setPred(matchingPredecessor);
                matchingPredecessor.setSucc(this);
            }
        } else if (this instanceof TextBlockVersion) {
            // consider text as caption for next code block --> focus on post blocks after this post block and the
            // matching predecessor  (post blocks before may be null)
            if ((beforeThis.getPred() == null || beforeMatch) && afterMatch) {
                setPred(matchingPredecessor);
                matchingPredecessor.setSucc(this);
            }
        }
    }

    @Transient
    public PostBlockVersion getSucc() {
        return succ;
    }

    public void setSucc(PostBlockVersion succ) {
        this.succ = succ;
    }

    @Transient
    public PostBlockVersion getRootPostBlock() {
        return rootPostBlock;
    }

    public void setRootPostBlock(PostBlockVersion rootPostBlock) {
        this.rootPostBlock = rootPostBlock;
    }

    @Transient
    public boolean isLifeSpanExtracted() {
        return lifeSpanExtracted;
    }

    public void setLifeSpanExtracted(boolean lifeSpanExtracted) {
        this.lifeSpanExtracted = lifeSpanExtracted;
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
            String msg = "A post block can only be predecessor of one post block in the next version (" + this + ")";
            logger.warning(msg);
            throw new IllegalStateException(msg);
        } else {
            isAvailable = false;
        }
    }

    abstract public double compareTo(PostBlockVersion otherBlock, Config config);

    double compareTo(PostBlockVersion otherBlock,
                     BiFunction<String, String, Double> similarityMetric,
                     BiFunction<String, String, Double> backupSimilarityMetric) {

        Double similarity;
        try {
            similarity = similarityMetric.apply(getContent(), otherBlock.getContent());
        } catch (InputTooShortException e) {
            if (backupSimilarityMetric != null) {
                similarity = backupSimilarityMetric.apply(getContent(), otherBlock.getContent());
            } else {
                throw e;
            }
        }

        if (Util.lessThan(similarity, 0.0) || Util.greaterThan(similarity, 1.0)) {
            String msg = "Similarity value must be in range [0.0, 1.0], but was " + similarity;
            logger.warning(msg);
            throw new IllegalSimilarityValueException(msg);
        }

        return similarity;
    }

    private List<diff_match_patch.Diff> diff(PostBlockVersion block) {
        return lineDiff.diff_lines_only(this.getContent(), block.getContent());
    }

    @Transient
    public List<PostBlockVersion> getMatchingPredecessors() {
        return matchingPredecessors;
    }

    @Transient
    public Map<PostBlockVersion, Double> getPredecessorSimilarities() {
        return predecessorSimilarities;
    }

    @Transient
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    @Transient
    public double getMaxSimilarity() {
        return maxSimilarity;
    }

    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks,
                                                                                        Config config,
                                                                                        Set<Integer> postBlockTypeFilter) {
        retrievePredecessorSimilarities(previousVersionPostBlocks, config, postBlockTypeFilter);
        retrieveMatchingPredecessors();
        return matchingPredecessors;
    }

    private <T extends PostBlockVersion> void retrievePredecessorSimilarities(
            List<T> previousVersionPostBlocks,
            Config config,
            Set<Integer> postBlockTypeFilter) {

        for (PostBlockVersion previousVersionPostBlock : previousVersionPostBlocks) {
            // apply post type filter
            if (!previousVersionPostBlock.isSelected(postBlockTypeFilter)) {
                continue;
            }

            // test equality
            boolean equal = getContent().equals(previousVersionPostBlock.getContent());

            // compare post block version and, if configured, catch InputTooShortExceptions
            double similarity;
            try {
                similarity = compareTo(previousVersionPostBlock, config);
            } catch (InputTooShortException e) {
                if (config.getCatchInputTooShortExceptions()) {
                    failedPredecessorsComparisons.add(previousVersionPostBlock);
                    similarity = 0.0;
                } else {
                    throw e;
                }
            }

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
    }

    private void retrieveMatchingPredecessors() {
        // retrieve predecessors with maximal similarity

        // return if maximum similarity is below the configured similarity threshold
        if (Util.lessThan(maxSimilarity, getSimilarityThreshold())) {
            return;
        }

        final double finalMaxSimilarity = maxSimilarity; // final value needed for lambda expression

        // get predecessors with max. similarity, sorted by similarity (may vary within Util.EPSILON)
        matchingPredecessors = predecessorSimilarities.entrySet()
                .stream()
                .filter(e -> Util.equals(e.getValue(), finalMaxSimilarity))
                .sorted((v1, v2) -> {
                    // sort descending according to similarity
                    int result = Double.compare(v2.getValue(), v1.getValue());
                    if (result == 0) {
                        // in case of same similarity, sort ascending according to local id
                        return Integer.compare(v1.getKey().getLocalId(), v2.getKey().getLocalId());
                    } else {
                        return result;
                    }
                }) // descending order
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // increase successor count for all matching predecessors
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            incrementPredCount();
            matchingPredecessor.incrementSuccCount();
        }
    }

    abstract public boolean isSelected(Set<Integer> postBlockTypeFilter);

    abstract public int getPostBlockTypeId();

    public static Set<Integer> getAllPostBlockTypeIdFilters() {
        return Sets.newHashSet(TextBlockVersion.postBlockTypeId, CodeBlockVersion.postBlockTypeId);
    }

    public Set<PostBlockVersion> getFailedPredecessorsComparisons() {
        return getFailedPredecessorsComparisons(getAllPostBlockTypeIdFilters());
    }

    public Set<PostBlockVersion> getFailedPredecessorsComparisons(Set<Integer> postBlockTypeFilter) {
        return failedPredecessorsComparisons.stream()
                .filter(b -> b.isSelected(postBlockTypeFilter))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "PostBlockVersion: " + getContent();
    }
}
