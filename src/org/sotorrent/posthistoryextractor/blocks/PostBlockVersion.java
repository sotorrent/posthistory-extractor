package org.sotorrent.posthistoryextractor.blocks;

import com.google.common.collect.Sets;
import org.sotorrent.posthistoryextractor.Config;
import org.sotorrent.posthistoryextractor.diffs.LineDiff;
import org.sotorrent.posthistoryextractor.diffs.diff_match_patch;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.util.LogUtils;
import org.sotorrent.util.MathUtils;
import org.sotorrent.util.exceptions.IllegalSimilarityValueException;
import org.sotorrent.util.exceptions.InputTooShortException;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Entity
@Table(name="PostBlockVersion")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PostBlockTypeId",discriminatorType=DiscriminatorType.INTEGER)
public abstract class PostBlockVersion {
    // eqal matches have this similarity
    public static final double EQUALITY_SIMILARITY = 10.0;
    // matches with similarity difference <= delta are considered to be equal
    private static final double UNIQUE_MATCH_DELTA = 0.05;

    public enum MatchingStrategy {BOTH, ABOVE, BELOW} // needed for method setPredContext

    private static Logger logger = null;
    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(PostBlockVersion.class, false);
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
    protected Integer postId;
    // since table PostBlockVersion is derived from table PostHistory, property PostTypeId is missing here
    // (but could be retrieved from table Posts if needed)
    protected Integer postHistoryId;
    protected Integer localId;
    protected Integer predPostBlockVersionId;
    protected Integer predPostHistoryId;
    protected Integer predLocalId;
    protected Integer rootPostBlockVersionId;
    protected Integer rootPostHistoryId;
    protected Integer rootLocalId;
    protected Boolean predEqual;
    protected Double predSimilarity;
    protected int predCount; // this marks possible predecessors, which may not be available for linking (see below)
    protected int succCount;
    protected int length;
    protected int lineCount;
    protected String content;
    // internal
    private StringBuilder contentBuilder;
    private LineDiff lineDiff;
    private List<diff_match_patch.Diff> predDiff;
    private PostBlockVersion pred;
    private PostBlockVersion succ;
    private PostBlockVersion rootPostBlock;
    private boolean isAvailable; // false if this post block is set as a predecessor of a block in the next version
    private List<PostBlockVersion> matchingPredecessors;
    private List<PostBlockVersion> matchingSuccessors;
    private List<PostBlockVersion> predecessorsAboveThreshold;
    private Map<PostBlockVersion, PostBlockSimilarity> predecessorSimilarities;
    private Map<PostBlockVersion, PostBlockSimilarity> successorsSimilarities;
    protected PostBlockSimilarity maxSimilarity;
    private boolean lifeSpanExtracted; // for extraction of PostBlockLifeSpan
    private Set<PostBlockVersion> failedPredecessorsComparisons; // needed for metrics comparison

    public PostBlockVersion() {
        // database
        this.postId = null;
        this.postHistoryId = null;
        this.localId = null;
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
        this.predPostBlockVersionId = null;
        this.predPostHistoryId = null;
        this.predLocalId = null;
        this.rootPostBlockVersionId = null;
        this.rootPostHistoryId = null;
        this.rootLocalId = null;
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
        this.matchingSuccessors = new LinkedList<>();
        this.predecessorsAboveThreshold = new LinkedList<>();
        this.predecessorSimilarities = new HashMap<>();
        this.successorsSimilarities = new HashMap<>();
        this.maxSimilarity = new PostBlockSimilarity();
        this.lifeSpanExtracted = false;
        this.failedPredecessorsComparisons = new HashSet<>();
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
    @Column(name = "PostHistoryId")
    public Integer getPostHistoryId() {
        return postHistoryId;
    }

    public void setPostHistoryId(Integer postHistoryId) {
        this.postHistoryId = postHistoryId;
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
    @Column(name = "PredPostBlockVersionId")
    public Integer getPredPostBlockVersionId() {
        return predPostBlockVersionId;
    }

    public void setPredPostBlockVersionId(Integer predPostBlockVersionId) {
        this.predPostBlockVersionId = predPostBlockVersionId;
    }

    @Basic
    @Column(name = "PredLocalId")
    public Integer getPredLocalId() {
        return predLocalId;
    }

    public void setPredLocalId(Integer localId) {
        this.predLocalId = localId;
    }

    @Basic
    @Column(name = "PredPostHistoryId")
    public Integer getPredPostHistoryId() {
        return predPostHistoryId;
    }

    public void setPredPostHistoryId(Integer predPostHistoryId) {
        this.predPostHistoryId = predPostHistoryId;
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
    @Column(name = "RootPostHistoryId")
    public Integer getRootPostHistoryId() {
        return rootPostHistoryId;
    }

    public void setRootPostHistoryId(Integer rootPostHistoryId) {
        this.rootPostHistoryId = rootPostHistoryId;
    }

    @Basic
    @Column(name = "RootLocalId")
    public Integer getRootLocalId() {
        return rootLocalId;
    }

    public void setRootLocalId(Integer rootLocalId) {
        this.rootLocalId = rootLocalId;
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
    @Column(name = "Content")
    public String getContent() {
        return content == null ? contentBuilder.toString() : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Transient
    public PostBlockVersion getPred() {
        return pred;
    }

    @Transient
    public PostBlockSimilarity getMaxSimilarity() {
        return maxSimilarity;
    }

    private void setPred(PostBlockVersion pred) {
        try {
            // set predecessor, local id of predecessor, and post block id of predecessor
            this.pred = pred;
            this.predPostBlockVersionId = pred.getId();
            this.predPostHistoryId = pred.getPostHistoryId();
            this.predLocalId = pred.getLocalId();
            // set predecessor similarity
            if (maxSimilarity.getMetricResult() == EQUALITY_SIMILARITY) {
                this.predSimilarity = 1.0;
                setPredEqual(true);
            } else {
                this.predSimilarity = maxSimilarity.getMetricResult();
                setPredEqual(false);
            }
            // compute line-based diff to predecessor
            predDiff = diff(pred);
            // mark predecessor as not available
            pred.setNotAvailable();
        } catch (Exception e) {
            logger.warning("Unable to set predecessor " + pred);
            e.printStackTrace();
        }
    }


    public void setUniqueMatchingPred() {
        // check if only one matching predecessor exists
        if (matchingPredecessors.size() == 1) {
            PostBlockVersion matchingPredecessor = matchingPredecessors.get(0);

            // the unique matching predecessor must be available
            if (matchingPredecessor.isAvailable()) {
                // check if the matching predecessor has this post block as successor with max similarity
                // get max similarity of matchingPredecessor's possible successors
                final double maxSim = matchingPredecessor.getSuccessorSimilarities().values().stream()
                        .map(PostBlockSimilarity::getMetricResult)
                        .max(Double::compareTo)
                        .orElseThrow(IllegalStateException::new);
                List<PostBlockVersion> successorsWithMaxSimilarity = matchingPredecessor.getSuccessorSimilarities().entrySet().stream()
                        .filter(entry -> MathUtils.lessThan(maxSim - entry.getValue().getMetricResult(), UNIQUE_MATCH_DELTA))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                if (successorsWithMaxSimilarity.size() == 1 && successorsWithMaxSimilarity.get(0) == this) {
                    setPred(matchingPredecessor);
                    matchingPredecessor.setSucc(this);
                }
            } else {
                // unique matching predecessor existed, but was not available
                setPredRunnerUp();
            }
        }
    }

    /**
     * Set predecessor that doesn't have highest similarity, but is available.
     */
    public void setPredRunnerUp() {
        // check if the next available predecessor with the next-highest similarity is also a unique match
        List<PostBlockVersion> runnerUpPredecessors = predecessorsAboveThreshold.stream()
                .filter(predecessor -> predecessor.isAvailable() & MathUtils.lessThan(
                        predecessorSimilarities.get(predecessor).getMetricResult(),
                        maxSimilarity.getMetricResult())
                )
                .sorted((pred1, pred2) -> Double.compare(
                        predecessorSimilarities.get(pred2).getMetricResult(),
                        predecessorSimilarities.get(pred1).getMetricResult()) // sort descending
                ).collect(Collectors.toList());

        if (runnerUpPredecessors.size() > 0) {
            PostBlockVersion bestMatch = runnerUpPredecessors.get(0);

            // if best match is unique or has highest similarity
            if (runnerUpPredecessors.size() == 1 ||
                    !MathUtils.greaterThan(
                            predecessorSimilarities.get(bestMatch).getMetricResult(),
                            predecessorSimilarities.get(runnerUpPredecessors.get(1)).getMetricResult()
                    )) {

                boolean matchingSuccessorsAvailable = bestMatch.getMatchingSuccessors().stream()
                        .anyMatch(succ -> succ.getPred() == null);

                if (bestMatch.getMatchingSuccessors().size() == 0 || !matchingSuccessorsAvailable) {
                    // update successor information, because this post block is not recognized as a matching successor yet
                    bestMatch.incrementSuccCount();
                    bestMatch.getMatchingSuccessors().add(this);
                    // update pred and succ pointers
                    setPred(bestMatch);
                    bestMatch.setSucc(this);
                }
            }
        }
    }

    public void setPredPosition() {
        // set matching predecessor that has a local id most similar to this block (and is still available)

        // sort available matching predecessors by their local id difference to this post block
        List<PostBlockVersion> matchingPredecessorsByLocalIdDiff = matchingPredecessors.stream()
                .filter(PostBlockVersion::isAvailable)
                .sorted(getPostBlockLocalIdComparator(localId))
                .collect(Collectors.toList());

        // check whether this post block has the smallest local id difference of all successors of a matching predecessor
        for (PostBlockVersion currentMatchingPredecessor : matchingPredecessorsByLocalIdDiff) {
            PostBlockVersion successorCandidate = currentMatchingPredecessor.getMatchingSuccessors().stream()
                    .filter(successor -> successor.getPred() == null)
                    .min(getPostBlockLocalIdComparator(currentMatchingPredecessor.getLocalId()))
                    .orElse(null);

            if (successorCandidate == this) {
                // this post block has the smallest local id difference of all possible successors
                setPred(currentMatchingPredecessor);
                currentMatchingPredecessor.setSucc(this);
                return;
            }
        }
    }

    private Comparator<PostBlockVersion> getPostBlockLocalIdComparator(int localId) {
        return (b1, b2) -> {
            int diffB1 = b1.getLocalId() - localId;
            int diffB2 = b2.getLocalId() - localId;
            int diffB1Abs = Math.abs(diffB1);
            int diffB2Abs = Math.abs(diffB2);
            if (diffB1Abs == diffB2Abs) {
                // if both blocks have the same distance to this block, chose the block with the smaller localId
                return Integer.compare(b1.getLocalId(), b2.getLocalId());
            } else {
                // otherwise, chose the block with the smaller difference to this block's localId
                return Integer.compare(diffB1Abs, diffB2Abs);
            }
        };
    }

    public boolean setPredContext(PostVersion currentVersion,
                               PostVersion previousVersion,
                               MatchingStrategy matchingStrategy) {
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            boolean matched = setPredContext(matchingPredecessor, currentVersion, previousVersion, matchingStrategy);
            if (matched) {
                return true;
            }
        }

        return false;
    }

    public boolean setPredContext(PostBlockVersion matchingPredecessor,
                               PostVersion currentVersion,
                               PostVersion previousVersion,
                               MatchingStrategy matchingStrategy) {
        // consider context to select matching predecessor

        // if the matching predecessor is not available, it cannot be set
        if (!matchingPredecessor.isAvailable()) {
            return false;
        }

        // retrieve context of this post block and the matching predecessor
        int indexThis = currentVersion.getPostBlocks().indexOf(this);
        int indexPred = previousVersion.getPostBlocks().indexOf(matchingPredecessor);

        // check if neighbors of this post block are available
        boolean aboveAvailableThis = indexThis > 0;
        boolean belowAvailableThis = indexThis < currentVersion.getPostBlocks().size() - 1;

        // check if neighbors of the predecessor are available
        boolean aboveAvailablePred = indexPred > 0;
        boolean belowAvailablePred = indexPred < previousVersion.getPostBlocks().size() - 1;

        // flags indicating if the matching predecessor has same neighbors (pred references set in previous step)
        boolean aboveMatch = false;
        boolean belowMatch = false;

        if (matchingStrategy == MatchingStrategy.BOTH || matchingStrategy == MatchingStrategy.ABOVE) {
            if (!aboveAvailableThis || !aboveAvailablePred) {
                return false;
            }
            PostBlockVersion aboveThis = currentVersion.getPostBlocks().get(indexThis - 1);
            PostBlockVersion abovePred = previousVersion.getPostBlocks().get(indexPred - 1);
            aboveMatch = aboveThis.getPred() != null && aboveThis.getPred() == abovePred;
        }

        if (matchingStrategy == MatchingStrategy.BOTH || matchingStrategy == MatchingStrategy.BELOW) {
            if (!belowAvailableThis || !belowAvailablePred) {
                return false;
            }
            PostBlockVersion belowThis = currentVersion.getPostBlocks().get(indexThis + 1);
            PostBlockVersion belowPred = previousVersion.getPostBlocks().get(indexPred + 1);
            belowMatch = belowThis.getPred() != null && belowThis.getPred() == belowPred;
        }

        // set the predecessor according the the provided matching strategy
        switch (matchingStrategy) {
            case BOTH: {
                if (aboveMatch && belowMatch) {
                    setPred(matchingPredecessor);
                    matchingPredecessor.setSucc(this);
                    return true;
                }
                break;
            }
            case ABOVE: {
                if (aboveMatch) {
                    setPred(matchingPredecessor);
                    matchingPredecessor.setSucc(this);
                    return true;
                }
                break;
            }
            case BELOW: {
                if (belowMatch) {
                    setPred(matchingPredecessor);
                    matchingPredecessor.setSucc(this);
                    return true;
                }
                break;
            }
        }

        return false;
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
        this.rootPostBlockVersionId = rootPostBlock.getId();
        this.rootPostHistoryId = rootPostBlock.getPostHistoryId();
        this.rootLocalId = rootPostBlock.getLocalId();
    }

    @Transient
    public boolean isLifeSpanExtracted() {
        return lifeSpanExtracted;
    }

    public void setLifeSpanExtracted(boolean lifeSpanExtracted) {
        this.lifeSpanExtracted = lifeSpanExtracted;
    }

    public void prepend(String line) {
        if (contentBuilder.length() == 0) {
            if (line.length() == 0) {
                contentBuilder = new StringBuilder("\n");
            } else {
                contentBuilder = new StringBuilder(line);
            }
        } else {
            if (line.length() == 0) {
                contentBuilder = new StringBuilder("\n" + contentBuilder.toString());
            } else {
                contentBuilder = new StringBuilder(line + "\n" + contentBuilder.toString());
            }
        }

        this.length++;
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

    @Transient
    abstract public PostBlockSimilarity compareTo(PostBlockVersion otherBlock, Config config);

    PostBlockSimilarity compareTo(PostBlockVersion otherBlock,
                     BiFunction<String, String, Double> similarityMetric,
                     BiFunction<String, String, Double> backupSimilarityMetric) {

        PostBlockSimilarity similarity;
        try {
            similarity = new PostBlockSimilarity(
                    similarityMetric.apply(getContent(), otherBlock.getContent()),
                    false
            );
        } catch (InputTooShortException e) {
            if (backupSimilarityMetric != null) {
                similarity = new PostBlockSimilarity(
                        backupSimilarityMetric.apply(getContent(), otherBlock.getContent()),
                        true
                );
            } else {
                throw e;
            }
        }

        if (MathUtils.lessThan(similarity.getMetricResult(), 0.0) || MathUtils.greaterThan(similarity.getMetricResult(), 1.0)) {
            String msg = "Metric result must be in range [0.0, 1.0], but was " + similarity.getMetricResult();
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
    public Map<PostBlockVersion, PostBlockSimilarity> getPredecessorSimilarities() {
        return predecessorSimilarities;
    }

    @Transient
    public List<PostBlockVersion> getMatchingSuccessors() {
        return matchingSuccessors;
    }

    @Transient
    public Map<PostBlockVersion, PostBlockSimilarity> getSuccessorSimilarities() {
        return successorsSimilarities;
    }

    @Transient
    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks,
                                                                                        Config config,
                                                                                        Set<Byte> postBlockTypeFilter) {
        retrievePredecessorSimilarities(previousVersionPostBlocks, config, postBlockTypeFilter);
        retrieveMatchingPredecessors(config);
        return matchingPredecessors;
    }

    private <T extends PostBlockVersion> void retrievePredecessorSimilarities(
            List<T> previousVersionPostBlocks,
            Config config,
            Set<Byte> postBlockTypeFilter) {

        for (PostBlockVersion previousVersionPostBlock : previousVersionPostBlocks) {
            // apply post type filter
            if (!previousVersionPostBlock.isSelected(postBlockTypeFilter)) {
                continue;
            }

            // only compare post blocks of same type
            if (!getPostBlockTypeId().equals(previousVersionPostBlock.getPostBlockTypeId())) {
                continue;
            }

            PostBlockSimilarity similarity;
            // test equality
            boolean equal = getContent().equals(previousVersionPostBlock.getContent());

            if (equal) {
                // equal predecessors have similarity 10.0 (see final static constant EQUALITY_SIMILARITY)
                similarity = new PostBlockSimilarity(EQUALITY_SIMILARITY);
                predecessorSimilarities.put(previousVersionPostBlock, similarity);
                maxSimilarity = similarity;
            } else {
                // compare post block version and, if configured, catch InputTooShortExceptions
                try {
                    similarity = compareTo(previousVersionPostBlock, config);
                } catch (InputTooShortException e) {
                    if (config.getCatchInputTooShortExceptions()) {
                        failedPredecessorsComparisons.add(previousVersionPostBlock);
                        similarity = new PostBlockSimilarity();
                    } else {
                        throw e;
                    }
                }

                predecessorSimilarities.put(previousVersionPostBlock, similarity);
                if (similarity.getMetricResult() > maxSimilarity.getMetricResult()) {
                    maxSimilarity = similarity;
                }
            }
        }
    }

    @Transient
    private Comparator<Map.Entry<PostBlockVersion, PostBlockSimilarity>> getSimilarityComparator() {
        return (v1, v2) -> {
            // sort descending according to similarity in descending order
            int result = Double.compare(v2.getValue().getMetricResult(), v1.getValue().getMetricResult());
            if (result == 0) {
                // in case of same similarity, sort ascending according to local id
                return Integer.compare(v1.getKey().getLocalId(), v2.getKey().getLocalId());
            } else {
                return result;
            }
        };
    }

    abstract void retrieveMatchingPredecessors(Config config);

    void retrieveMatchingPredecessors(double similarityThreshold, double backupSimilarityThreshold) {
        // retrieve predecessors with maximal similarity

        // check if max similarity is below threshold has already been conducted in the subclasses (TextBlockVersion, CodeBlockVersion)

        // get max similarity, final value needed for lambda expression
        final double finalMaxSimilarity = maxSimilarity.getMetricResult();

        // get predecessors with max. similarity, sorted by similarity (may vary within Util.EPSILON)
        matchingPredecessors = predecessorSimilarities.entrySet()
                .stream()
                .filter(e -> MathUtils.equals(e.getValue().getMetricResult(), finalMaxSimilarity))
                .sorted(getSimilarityComparator())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // increase successor count for all matching predecessors
        for (PostBlockVersion matchingPredecessor : matchingPredecessors) {
            incrementPredCount();
            matchingPredecessor.incrementSuccCount();
        }

        predecessorsAboveThreshold = predecessorSimilarities.entrySet()
                .stream()
                .filter(e -> {
                    if (maxSimilarity.isBackupSimilarity()) {
                        return e.getValue().getMetricResult() >= backupSimilarityThreshold;
                    } else {
                        return e.getValue().getMetricResult() >= similarityThreshold;
                    }
                })
                .sorted(getSimilarityComparator())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Transient
    abstract public boolean isSelected(Set<Byte> postBlockTypeFilter);

    @Transient
    abstract public Byte getPostBlockTypeId();

    public static Set<Byte> getAllPostBlockTypeIdFilters() {
        return Sets.newHashSet(TextBlockVersion.postBlockTypeId, CodeBlockVersion.postBlockTypeId);
    }

    @Transient
    public Set<PostBlockVersion> getFailedPredecessorsComparisons() {
        return getFailedPredecessorsComparisons(getAllPostBlockTypeIdFilters());
    }

    @Transient
    public Set<PostBlockVersion> getFailedPredecessorsComparisons(Set<Byte> postBlockTypeFilter) {
        return failedPredecessorsComparisons.stream()
                .filter(b -> b.isSelected(postBlockTypeFilter))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "PostBlockVersion: " + getContent();
    }
}
