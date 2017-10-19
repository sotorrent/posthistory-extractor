package de.unitrier.st.soposthistory.urls;

import java.util.regex.Pattern;

public class MarkdownLinkBareTags {

    // Source: https://stackoverflow.com/editing-help#code
    // Example 1: I often visit http://example.com.
    // Example 2: Have you seen <http://example.com>?

    // Source: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#links
    // URLs and URLs in angle brackets will automatically get turned into links.
    // http://www.example.com or <http://www.example.com> and sometimes
    // example.com (but not on Github, for example).

    public static final Pattern regex = Pattern.compile("<((?:http|ftp|https):\\/\\/(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-]))>");
}
