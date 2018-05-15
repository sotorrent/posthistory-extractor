package de.unitrier.st.soposthistory.urls;

import de.unitrier.st.util.Patterns;

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


    //public static final Pattern regex = Pattern.compile("\\[[^]]+]:\\s*((?:http|ftp|https)://(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-]))?");
    private static final Pattern regex_usages = Pattern.compile("\\[([^]]*)]\\[(\\s*.*?\\s*)]");
    private static final Pattern regex_definitions = Pattern.compile("(?:\\[([^]]+)]:\\s*(" + Patterns.urlRegex + ")?)(?:\\s+\"(.*)\")?");

    public static List<Link> extract(String markdownContent) {
        LinkedList<MarkdownLinkReference> extractedLinks = new LinkedList<>();

        Matcher matcher = regex_usages.matcher(markdownContent);
        while (matcher.find()) {
            MarkdownLinkReference extractedLink = new MarkdownLinkReference();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.anchor = matcher.group(1);
            extractedLink.reference = matcher.group(2);
            extractedLinks.add(extractedLink);
        }

        matcher = regex_definitions.matcher(markdownContent);
        while (matcher.find()) {
            MarkdownLinkReference extractedLink = new MarkdownLinkReference();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.reference = matcher.group(1);
            extractedLink.url = matcher.group(2);
            extractedLink.title = matcher.group(3);
            extractedLinks.add(extractedLink);
        }

        return mergeUsagesAndDefinitions(extractedLinks);
    }

    private static List<Link> mergeUsagesAndDefinitions(List<MarkdownLinkReference> extractedLinks) {
        LinkedList<Link> mergedLinks = new LinkedList<>();

        for (Link link1 : extractedLinks) {
            if (link1.anchor != null && link1.url == null && link1.reference != null) {
                // link is usage of link reference
                for (Link link2 : extractedLinks) {
                    if (link2.anchor == null && link2.url != null && link2.reference != null) {
                        if (link1.reference.equals(link2.reference)) {
                            MarkdownLinkReference mergedLink = new MarkdownLinkReference();
                            mergedLink.reference = link1.reference;
                            mergedLink.anchor = link1.anchor;
                            mergedLink.title = link2.title;
                            mergedLink.url = link2.url;
                            mergedLink.fullMatch = link1.fullMatch + "\n" + link2.fullMatch;
                            if (mergedLink.url != null && mergedLink.url.length() > 0) {
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
