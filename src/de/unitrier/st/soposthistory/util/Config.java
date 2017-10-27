package de.unitrier.st.soposthistory.util;

import java.util.function.BiFunction;

public class Config {
    // text blocks
    private final BiFunction<String, String, Double> textSimilarityMetric;
    private final BiFunction<String, String, Double> textBackupSimilarityMetric;
    private final double textSimilarityThreshold;
    // code blocks
    private final BiFunction<String, String, Double> codeSimilarityMetric;
    private final BiFunction<String, String, Double> codeBackupSimilarityMetric;
    private final double codeSimilarityThreshold;

    private Config(BiFunction<String, String, Double> textSimilarityMetric,
                  BiFunction<String, String, Double> textBackupSimilarityMetric,
                  double textSimilarityThreshold,
                  BiFunction<String, String, Double> codeSimilarityMetric,
                  BiFunction<String, String, Double> codeBackupSimilarityMetric,
                  double codeSimilarityThreshold) {
        this.textSimilarityMetric = textSimilarityMetric;
        this.textBackupSimilarityMetric = textBackupSimilarityMetric;
        this.textSimilarityThreshold = textSimilarityThreshold;
        this.codeSimilarityMetric = codeSimilarityMetric;
        this.codeBackupSimilarityMetric = codeBackupSimilarityMetric;
        this.codeSimilarityThreshold = codeSimilarityThreshold;
    }

    public static final Config EMPTY = new Config(
            (str1, str2) -> 0.0,
            null,
            Double.POSITIVE_INFINITY,
            (str1, str2) -> 0.0,
            null,
            Double.POSITIVE_INFINITY
    );

    //TODO: update this after evaluation
    public static final Config DEFAULT = new Config(
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

    public double getCodeSimilarityThreshold() {
        return codeSimilarityThreshold;
    }

    public Config withTextSimilarityMetric(BiFunction<String, String, Double> textSimilarityMetric){
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withTextBackupSimilarityMetric(BiFunction<String, String, Double> textBackupSimilarityMetric){
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withTextSimilarityThreshold(double textSimilarityThreshold){
        if (textSimilarityThreshold < 0.0 || textSimilarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeSimilarityMetric(BiFunction<String, String, Double> codeSimilarityMetric){
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeBackupSimilarityMetric(BiFunction<String, String, Double> codeBackupSimilarityMetric){
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }

    public Config withCodeSimilarityThreshold(double codeSimilarityThreshold){
        if (textSimilarityThreshold < 0.0 || textSimilarityThreshold > 1.0) {
            throw new IllegalArgumentException("Similarity threshold must be in range [0.0, 1.0]");
        }
        return new Config(textSimilarityMetric, textBackupSimilarityMetric, textSimilarityThreshold,
                codeSimilarityMetric, codeBackupSimilarityMetric, codeSimilarityThreshold);
    }
}
