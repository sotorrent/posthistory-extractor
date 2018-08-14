package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.Patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownLinkAngleBrackets extends Link {

    // Source: https://stackoverflow.com/editing-help#code
    // Example 1: I often visit http://example.com.
    // Example 2: Have you seen <http://example.com>?

    // Source: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#links
    // URLs and URLs in angle brackets will automatically get turned into links.
    // http://www.example.com or <http://www.example.com> and sometimes
    // example.com (but not on Github, for example).

    private static final Pattern pattern = Pattern.compile("<(" + Patterns.urlRegex + ")>", Pattern.CASE_INSENSITIVE);

    public static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = pattern.matcher(markdownContent);

        while (matcher.find()) {
            if (Patterns.inInlineCode(matcher, markdownContent)) {
                continue;
            }
            MarkdownLinkAngleBrackets extractedLink = new MarkdownLinkAngleBrackets();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.setUrl(matcher.group(1));
            if (extractedLink.url != null && extractedLink.url.length() > 0) {
                extractedLinks.add(extractedLink);
            }
        }

        return extractedLinks;
    }

}
