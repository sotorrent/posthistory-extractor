package de.unitrier.st.soposthistory.util;

import java.util.function.BiFunction;

public class Config {
    // configure post history processing
    private final boolean extractUrls;
    private final boolean computeDiffs;

    // metrics and threshold for text blocks
    private final BiFunction<String, String, Double> textSimilarityMetric;
    private final BiFunction<String, String, Double> textBackupSimilarityMetric;
    private final double textSimilarityThreshold;

    // metrics and threshold for code blocks
    private final BiFunction<String, String, Double> codeSimilarityMetric;
    private final BiFunction<String, String, Double> codeBackupSimilarityMetric;
    private final double codeSimilarityThreshold;

    private Config(boolean extractUrls, boolean computeDiffs,
                   BiFunction<String, String, Double> textSimilarityMetric,
                   BiFunction<String, String, Double> textBackupSimilarityMetric,
                   double textSimilarityThreshold,
                   BiFunction<String, String, Double> codeSimilarityMetric,
                   BiFunction<String, String, Double> codeBackupSimilarityMetric,
                   double codeSimilarityThreshold) {
        this.extractUrls = extractUrls;
        this.computeDiffs = computeDiffs;
        this.textSimilarityMetric = textSimilarityMetric;
        this.textBackupSimilarityMetric = textBackupSimilarityMetric;
        this.textSimilarityThreshold = textSimilarityThreshold;
        this.codeSimilarityMetric = codeSimilarityMetric;
        this.codeBackupSimilarityMetric = codeBackupSimilarityMetric;
        this.codeSimilarityThreshold = codeSimilarityThreshold;
    }

    public static final Config EMPTY = new Config(
            false,
            false,
            (str1, str2) -> 0.0,
            null,
            Double.POSITIVE_INFINITY,
            (str1, str2) -> 0.0,
            null,
            Double.POSITIVE_INFINITY
    );

    public static final Config METRICS_COMPARISON = new Config(
            false,
            false,
            (str1, str2) -> 0.0,
            null,
            0.6,
            (str1, str2) -> 0.0,
            null,
            0.6
    );

    //TODO: update this after evaluation
    public static final Config DEFAULT = new Config(
            true,
            true,
            de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap,
            de.unitrier.st.stringsimilarity.edit.Variants::levenshtein,
            0.6,
            de.unitrier.st.stringsimilarity.set.Variants::fourGramOverlap,
            de.unitrier.st.stringsimilarity.edit.Variants::levenshtein,
            0.6
    );

    public BiFunction<String, String, Double> getTextSimilarityMetric() {
        return textSimilarityMetric;
    }

    public BiFunction<String, String, Double> getTextBackupSimilarityMetric() {
        return textBackupSimilarityMetric;
    }

    public double getTextSimilarityThreshold() {
        return textSimilarityThreshold;
    }

    public BiFunction<String, String, Double> getCodeSimilarityMetric() {
        return codeSimilarityMetric;
    }

    public BiFunction<String, String, Double> getCodeBackupSimilarityMetric() {
        return codeBackupSimilarityMetric;
    }

    public boolean getExtractUrls() {
        return extractUrls;
    }

    public boolean getComputeDiffs() {
        return computeDiffs;
    }

    public double getCodeSimilarityThreshold() {
        return codeSimilarityThreshold;
    }

    public Config withExtractUrls(boolean extractUrls) {
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withComputeDiffs(boolean computeDiffs) {
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withTextSimilarityMetric(BiFunction<String, String, Double> textSimilarityMetric){
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withTextBackupSimilarityMetric(BiFunction<String, String, Double> textBackupSimilarityMetric){
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withTextSimilarityThreshold(double textSimilarityThreshold){
        if (Util.lessThan(textSimilarityThreshold, 0.0)
                || Util.greaterThan(textSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeSimilarityMetric(BiFunction<String, String, Double> codeSimilarityMetric){
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeBackupSimilarityMetric(BiFunction<String, String, Double> codeBackupSimilarityMetric){
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeSimilarityThreshold(double codeSimilarityThreshold){
        if (Util.lessThan(codeSimilarityThreshold, 0.0)
                || Util.greaterThan(codeSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs,
                textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }
}
