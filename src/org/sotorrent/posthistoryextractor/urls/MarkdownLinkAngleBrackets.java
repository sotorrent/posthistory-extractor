package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.URL;

import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
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

    private static final Pattern pattern = Pattern.compile("<(" + URL.urlRegex + ")>", Pattern.CASE_INSENSITIVE);

    static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = pattern.matcher(markdownContent);

        while (matcher.find()) {
            if (URL.inInlineCode(matcher, markdownContent)) {
                continue;
            }
            String url = matcher.group(1);
            try {
                MarkdownLinkAngleBrackets extractedLink = new MarkdownLinkAngleBrackets();
                extractedLink.fullMatch = matcher.group(0);
                extractedLink.setUrl(url);
                if (!extractedLink.getUrlObject().isEmpty()) {
                    extractedLinks.add(extractedLink);
                }
            } catch (MalformedURLException e) {
                logger.warning("Malformed " + MethodHandles.lookup().lookupClass() + " URL: " + url);
            }
        }

        return extractedLinks;
    }

}
