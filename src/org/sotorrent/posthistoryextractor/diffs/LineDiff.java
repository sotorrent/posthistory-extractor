package org.sotorrent.posthistoryextractor.diffs;

import java.util.LinkedList;

public class LineDiff extends diff_match_patch {

    public LinkedList<Diff> diff_lines_only(String text1, String text2) {
        final LinesToCharsResult lines = diff_linesToChars(text1, text2);
        //lines.chars1 etc. are not accessible from outside the package
        final LinkedList<Diff> diffs = diff_main(lines.chars1, lines.chars2);

        diff_charsToLines(diffs, lines.lineArray);

        return diffs;
    }

    static int operationToInt(Operation op) {
        switch (op) {
            case DELETE: return -1;
            case EQUAL: return 0;
            case INSERT: return 1;
            default: throw new IllegalArgumentException("Unknown operation.");
        }
    }

}
