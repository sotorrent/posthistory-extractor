package de.unitrier.st.soposthistory;

import de.unitrier.st.util.Util;

import java.util.function.BiFunction;

public class Config {
    // configure post history processing
    private final boolean extractUrls;
    private final boolean computeDiffs;
    private final boolean catchInputTooShortExceptions;

    // metrics and threshold for text blocks
    private final BiFunction<String, String, Double> textSimilarityMetric;
    private final double textSimilarityThreshold;
    private final BiFunction<String, String, Double> textBackupSimilarityMetric;
    private final double textBackupSimilarityThreshold;

    // metrics and threshold for code blocks
    private final BiFunction<String, String, Double> codeSimilarityMetric;
    private final double codeSimilarityThreshold;
    private final BiFunction<String, String, Double> codeBackupSimilarityMetric;
    private final double codeBackupSimilarityThreshold;

    private Config(boolean extractUrls, boolean computeDiffs, boolean catchInputTooShortExceptions,
                   BiFunction<String, String, Double> textSimilarityMetric,
                   double textSimilarityThreshold,
                   BiFunction<String, String, Double> textBackupSimilarityMetric,
                   double textBackupSimilarityThreshold,
                   BiFunction<String, String, Double> codeSimilarityMetric,
                   double codeSimilarityThreshold,
                   BiFunction<String, String, Double> codeBackupSimilarityMetric,
                   double codeBackupSimilarityThreshold) {
        this.extractUrls = extractUrls;
        this.computeDiffs = computeDiffs;
        this.catchInputTooShortExceptions = catchInputTooShortExceptions;
        this.textSimilarityMetric = textSimilarityMetric;
        this.textSimilarityThreshold = textSimilarityThreshold;
        this.textBackupSimilarityMetric = textBackupSimilarityMetric;
        this.textBackupSimilarityThreshold = textBackupSimilarityThreshold;
        this.codeSimilarityMetric = codeSimilarityMetric;
        this.codeSimilarityThreshold = codeSimilarityThreshold;
        this.codeBackupSimilarityMetric = codeBackupSimilarityMetric;
        this.codeBackupSimilarityThreshold = codeBackupSimilarityThreshold;
    }

    public static final Config EMPTY = new Config(
            false,
            false,
            false,
            (str1, str2) -> 0.0,
            Double.POSITIVE_INFINITY,
            null,
            Double.POSITIVE_INFINITY,
            (str1, str2) -> 0.0,
            Double.POSITIVE_INFINITY,
            null,
            Double.POSITIVE_INFINITY
    );

    public static final Config METRICS_COMPARISON = new Config(
            false,
            false,
            true,
            (str1, str2) -> 0.0,
            1.0,
            null,
            1.0,
            (str1, str2) -> 0.0,
            1.0,
            null,
            1.0
    );

    public static final Config DEFAULT = new Config(
            true,
            true,
            false,
            de.unitrier.st.stringsimilarity.profile.Variants::manhattanFourGramNormalized,
            0.17,
            de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedTermFrequency,
            0.36,
            de.unitrier.st.stringsimilarity.fingerprint.Variants::winnowingFourGramDiceNormalized,
            0.23,
            de.unitrier.st.stringsimilarity.profile.Variants::cosineTokenNormalizedNormalizedTermFrequency,
            0.26
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

    public double getTextBackupSimilarityThreshold() {
        return textBackupSimilarityThreshold;
    }

    public BiFunction<String, String, Double> getCodeSimilarityMetric() {
        return codeSimilarityMetric;
    }

    public BiFunction<String, String, Double> getCodeBackupSimilarityMetric() {
        return codeBackupSimilarityMetric;
    }

    public double getCodeSimilarityThreshold() {
        return codeSimilarityThreshold;
    }

    public double getCodeBackupSimilarityThreshold() {
        return codeBackupSimilarityThreshold;
    }

    public boolean getExtractUrls() {
        return extractUrls;
    }

    public boolean getComputeDiffs() {
        return computeDiffs;
    }

    public boolean getCatchInputTooShortExceptions() {
        return catchInputTooShortExceptions;
    }

    public Config withExtractUrls(boolean extractUrls) {
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withComputeDiffs(boolean computeDiffs) {
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withCatchInputTooShortExceptions(boolean catchInputTooShortExceptions) {
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withTextSimilarityMetric(BiFunction<String, String, Double> textSimilarityMetric){
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withTextSimilarityThreshold(double textSimilarityThreshold){
        if (Util.lessThan(textSimilarityThreshold, 0.0)
                || Util.greaterThan(textSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withTextBackupSimilarityMetric(BiFunction<String, String, Double> textBackupSimilarityMetric){
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withTextBackupSimilarityThreshold(double textBackupSimilarityThreshold){
        if (Util.lessThan(textBackupSimilarityThreshold, 0.0)
                || Util.greaterThan(textBackupSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withCodeSimilarityMetric(BiFunction<String, String, Double> codeSimilarityMetric){
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withCodeSimilarityThreshold(double codeSimilarityThreshold){
        if (Util.lessThan(codeSimilarityThreshold, 0.0)
                || Util.greaterThan(codeSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withCodeBackupSimilarityMetric(BiFunction<String, String, Double> codeBackupSimilarityMetric){
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }

    public Config withCodeBackupSimilarityThreshold(double codeBackupSimilarityThreshold){
        if (Util.lessThan(codeBackupSimilarityThreshold, 0.0)
                || Util.greaterThan(codeBackupSimilarityThreshold, 1.0)) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(extractUrls, computeDiffs, catchInputTooShortExceptions,
                textSimilarityMetric, textSimilarityThreshold, textBackupSimilarityMetric, textBackupSimilarityThreshold,
                codeSimilarityMetric, codeSimilarityThreshold, codeBackupSimilarityMetric, codeBackupSimilarityThreshold);
    }
}
