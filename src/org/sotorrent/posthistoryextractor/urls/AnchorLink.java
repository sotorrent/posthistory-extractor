package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.Patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnchorLink extends Link {
    // Example: <a href="http://example.com" title="example">example</a>
    private static final Pattern regex = Pattern.compile("<a\\s+href\\s*=\\s*\"(" + Patterns.urlRegex + ")?\"(?:\\s+(?:title=\"(.*?)\"))?>(.*?)</a>", Pattern.CASE_INSENSITIVE);

    public static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = regex.matcher(markdownContent);

        while (matcher.find()) {
            AnchorLink extractedLink = new AnchorLink();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.setUrl(matcher.group(1));
            extractedLink.title = matcher.group(2);
            extractedLink.anchor = matcher.group(3);
            // e.g., <a href=""> </a>
            if (extractedLink.url != null && extractedLink.url.length() > 0) {
                extractedLinks.add(extractedLink);
            }
        }

        return extractedLinks;
    }
}
