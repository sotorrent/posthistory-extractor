package de.unitrier.st.soposthistory.urls;

import java.util.regex.Pattern;

public class MarkdownLinkReference {
    // TODO: adapt for include second matching group for title/label

    // Source: https://stackoverflow.com/editing-help#code
    // Example 1: Here's a reference-style link to [Google][1].   ...     and later   ...     [1]: http://www.google.com/
    // Example 2: Here's a very readable link to [Yahoo!][yahoo].      ...     and later   ...     [yahoo]: http://www.yahoo.com/

    // Source: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#links
    // Example 3:
    // [I'm a reference-style link][Arbitrary case-insensitive reference text]
    // [You can use numbers for reference-style link definitions][1]
    // Or leave it empty and use the [link text itself].
    // Some text to show that the reference links can follow later.
    // [arbitrary case-insensitive reference text]: https://www.mozilla.org
    // [1]: http://slashdot.org
    // [link text itself]: http://www.reddit.com

    public static final Pattern regex = Pattern.compile("\\[[^]]+]:\\s*((?:http|ftp|https)://(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-]))?");

}
