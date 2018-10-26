package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.URL;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownLinkReference extends Link {

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

    private static final Pattern patternUsages = Pattern.compile("\\[([^]]*)]\\[(\\s*.*?\\s*)]", Pattern.CASE_INSENSITIVE);
    static final Pattern patternDefinitions = Pattern.compile("(?:\\[([^]]+)]:\\s*(" + URL.urlRegex + ")?)(?:\\s+\"(.*)\")?", Pattern.CASE_INSENSITIVE);

    static List<Link> extract(String markdownContent) {
        LinkedList<MarkdownLinkReference> extractedLinks = new LinkedList<>();

        Matcher matcher = patternUsages.matcher(markdownContent);
        while (matcher.find()) {
            MarkdownLinkReference extractedLink = new MarkdownLinkReference();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.anchor = matcher.group(1);
            extractedLink.reference = matcher.group(2);
            extractedLinks.add(extractedLink);
        }

        List<Link> mergedLinks = new LinkedList<>();
        matcher = patternDefinitions.matcher(markdownContent);
        while (matcher.find()) {
            MarkdownLinkReference extractedLink = new MarkdownLinkReference();
            String url = matcher.group(2);
            try {
                extractedLink.setUrl(url);
                extractedLink.fullMatch = matcher.group(0);
                extractedLink.reference = matcher.group(1);
                extractedLink.title = matcher.group(3);
                extractedLinks.add(extractedLink);
                mergedLinks = mergeUsagesAndDefinitions(extractedLinks);
            } catch (MalformedURLException e) {
                logger.info("Malformed URL: " + url);
            }
        }

        return mergedLinks;
    }

    private static List<Link> mergeUsagesAndDefinitions(List<MarkdownLinkReference> extractedLinks) throws MalformedURLException {
        List<Link> mergedLinks = new LinkedList<>();

        for (Link link1 : extractedLinks) {
            if (link1.anchor != null && link1.getUrlString() == null && link1.reference != null) {
                // link is usage of link reference
                for (Link link2 : extractedLinks) {
                    if (link2.anchor == null && link2.getUrlString() != null && link2.reference != null) {
                        if (link1.reference.equals(link2.reference)) {
                            MarkdownLinkReference mergedLink = new MarkdownLinkReference();
                            mergedLink.reference = link1.reference;
                            mergedLink.anchor = link1.anchor;
                            mergedLink.title = link2.title;
                            mergedLink.setUrl(link2.getUrlString());
                            mergedLink.fullMatch = link1.fullMatch + "\n" + link2.fullMatch;
                            if (mergedLink.getUrlString() != null && mergedLink.getUrlString().length() > 0) {
                                mergedLinks.add(mergedLink);
                            }
                        }
                    }

                }
            }
        }

        return mergedLinks;
    }
}
