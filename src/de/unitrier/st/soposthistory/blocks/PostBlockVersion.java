package de.unitrier.st.soposthistory.blocks;

import com.google.common.collect.Sets;
import de.unitrier.st.soposthistory.Config;
import de.unitrier.st.soposthistory.diffs.LineDiff;
import de.unitrier.st.soposthistory.diffs.diff_match_patch;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.util.IllegalSimilarityValueException;
import de.unitrier.st.util.InputTooShortException;
import de.unitrier.st.util.Util;

import javax.persistence.*;
import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Entity
@Table(name = "PostBlockVersion", schema = "stackoverflow16_12")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="PostBlockTypeId",discriminatorType=DiscriminatorType.INTEGER)
public abstract class PostBlockVersion {
    public static final double EQUALITY_SIMILARITY = 10.0;

    public enum MatchingStrategy {BOTH, ABOVE, BELOW} // needed for method setPredContext

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
    protected double maxSimilarity;
    protected double maxBackupSimilarity;
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
        this.maxBackupSimilarity = -1.0;
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

    @Transient
    public double getMaxSimilarity() {
        return maxSimilarity;
    }

    @Transient
    public double getMaxBackupSimilarity() {
        return maxBackupSimilarity;
    }

    private void setPred(PostBlockVersion pred) {
        try {
            // set pred and post block id of pred
            this.pred = pred;
            this.predPostBlockId = pred.getId();
            // set pred similarity
            if (maxSimilarity == EQUALITY_SIMILARITY) {
                this.predSimilarity = 1.0;
                setPredEqual(true);
            } else {
                this.predSimilarity = Math.max(maxSimilarity, maxBackupSimilarity);
                setPredEqual(false);
            }
            // compute line-based diff to pred
            predDiff = diff(pred);
            // mark predecessor as not available
            pred.setNotAvailable();
        } catch (Exception e) {
            logger.warning("Unable to set predecessor " + pred);
            e.printStackTrace();
        }
    }

    public void setUniqueMatchingPred(Map<PostBlockVersion, List<PostBlockVersion>> matchedPredecessors) {
        if (matchingPredecessors.size() == 1) {
            // only one matching predecessor found
            PostBlockVersion matchingPredecessor = matchingPredecessors.get(0);
            if (matchedPredecessors.get(matchingPredecessor).size() == 1 && matchingPredecessor.isAvailable()) {
                // the matched predecessor is only matched for currentPostBlock and is available
                setPred(matchingPredecessor);
                matchingPredecessor.setSucc(this);
            }
        }
    }

    public void setPredLocalId(Map<PostBlockVersion, List<PostBlockVersion>> matchedPredecessorsPreviousVersion) {
        // matchedPredecessors : matched predecessor -> list of successors
        // set matching predecessor that has a local id most similar to this block (and is still available)

        // only consider matched predecessors which are available and which are matching predecessors of this post block
        List<PostBlockVersion> matchedPostBlocksPreviousVersion = matchedPredecessorsPreviousVersion.keySet().stream()
                .filter(b -> b.isAvailable() && matchingPredecessors.contains(b))
                .sorted(Comparator.comparingInt(PostBlockVersion::getLocalId))
                .collect(Collectors.toList());

        // get all post blocks from this version that may be successors of the post blocks retrieved above
        List<PostBlockVersion> matchedPostBlocksCurrentVersion = matchedPredecessorsPreviousVersion.values().stream()
                .flatMap(List::stream)
                .filter(b -> b.getPred() == null)
                .collect(Collectors.toSet()) // only consider unique post blocks
                .stream()
                .sorted(Comparator.comparingInt(PostBlockVersion::getLocalId))
                .collect(Collectors.toList());

        // check whether this post block is the successor of one of the matched predecessor according to the localId difference
        for (PostBlockVersion matchedPostBlockPreviousVersion : matchedPostBlocksPreviousVersion) {
            PostBlockVersion successorCandidate = matchedPostBlocksCurrentVersion.stream()
                    .min(getPostBlockLocalIdComparator(matchedPostBlockPreviousVersion.getLocalId()))
                    .orElse(null);
            if (successorCandidate != this) {
                continue;
            }
            PostBlockVersion predecessorCandidate = matchedPostBlocksPreviousVersion.stream()
                    .min(getPostBlockLocalIdComparator(successorCandidate.getLocalId()))
                    .orElse(null);

            if (predecessorCandidate == matchedPostBlockPreviousVersion) {
                setPred(matchedPostBlockPreviousVersion);
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

    abstract public PostBlockSimilarity compareTo(PostBlockVersion otherBlock, Config config);

    PostBlockSimilarity compareTo(PostBlockVersion otherBlock,
                     BiFunction<String, String, Double> similarityMetric,
                     BiFunction<String, String, Double> backupSimilarityMetric) {

        PostBlockSimilarity similarity = new PostBlockSimilarity();
        try {
            similarity.setMetricResult(similarityMetric.apply(getContent(), otherBlock.getContent()));
            similarity.setBackupSimilarity(false);
        } catch (InputTooShortException e) {
            if (backupSimilarityMetric != null) {
                similarity.setMetricResult(backupSimilarityMetric.apply(getContent(), otherBlock.getContent()));
                similarity.setBackupSimilarity(true);
            } else {
                throw e;
            }
        }

        if (Util.lessThan(similarity.getMetricResult(), 0.0) || Util.greaterThan(similarity.getMetricResult(), 1.0)) {
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
    public Map<PostBlockVersion, Double> getPredecessorSimilarities() {
        return predecessorSimilarities;
    }

    public <T extends PostBlockVersion> List<PostBlockVersion> findMatchingPredecessors(List<T> previousVersionPostBlocks,
                                                                                        Config config,
                                                                                        Set<Integer> postBlockTypeFilter) {
        retrievePredecessorSimilarities(previousVersionPostBlocks, config, postBlockTypeFilter);
        retrieveMatchingPredecessors(config);
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

            // only compare post blocks of same type
            if (getPostBlockTypeId() != previousVersionPostBlock.getPostBlockTypeId()) {
                continue;
            }

            // test equality
            boolean equal = getContent().equals(previousVersionPostBlock.getContent());

            if (equal) {
                // equal predecessors have similarity 10.0 (see final static constant EQUALITY_SIMILARITY)
                predecessorSimilarities.put(previousVersionPostBlock, EQUALITY_SIMILARITY);
                maxSimilarity = EQUALITY_SIMILARITY;
            } else {
                // compare post block version and, if configured, catch InputTooShortExceptions
                PostBlockSimilarity similarity;
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

                predecessorSimilarities.put(previousVersionPostBlock, similarity.getMetricResult());
                if (similarity.isBackupSimilarity()) {
                    if (similarity.getMetricResult() > maxBackupSimilarity) {
                        maxBackupSimilarity = similarity.getMetricResult();
                    }
                } else {
                    if (similarity.getMetricResult() > maxSimilarity) {
                        maxSimilarity = similarity.getMetricResult();
                    }
                }
            }
        }
    }

    abstract void retrieveMatchingPredecessors(Config config);

    void retrieveMatchingPredecessors(double similarityThreshold, double backupSimilarityThreshold) {
        // retrieve predecessors with maximal similarity

        // threshold check already conducted in subclasses (TextBlockVersion, CodeBlockVersion)

        // get max similarity, final value needed for lambda expression
        final double finalMaxSimilarity = Math.max(maxSimilarity, maxBackupSimilarity);

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
