package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.Patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownLinkInline extends Link {

    // Source: https://stackoverflow.com/editing-help#code
    // Example 1: Here's an inline link to [Google](http://www.google.com/).
    // Example 2: [poorly-named link](http://www.google.com/ "Google").

    // Source: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#links
    // Example 3: [I'm an inline-style link](https://www.google.com)
    // Example 4: [I'm an inline-style link with title](https://www.google.com "Google's Homepage")

    private static final Pattern regex = Pattern.compile("\\[([^]]+)]\\(\\s*(" + Patterns.urlRegex + ")?(?:\\s+\"([^\"]+)\")?\\s*\\)", Pattern.CASE_INSENSITIVE);

    public static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = regex.matcher(markdownContent);

        while (matcher.find()) {
            MarkdownLinkInline extractedLink = new MarkdownLinkInline();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.anchor = matcher.group(1);
            extractedLink.setUrl(matcher.group(2));
            extractedLink.title = matcher.group(3);
            // e.g., [`tr///`]()
            if (extractedLink.url != null && extractedLink.url.length() > 0) {
                extractedLinks.add(extractedLink);
            }
        }

        return extractedLinks;
    }

}
