package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.URL;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnchorLink extends Link {
    // Example: <a href="http://example.com" title="example">example</a>
    private static final Pattern pattern = Pattern.compile("<a\\s+href\\s*=\\s*\"(" + URL.urlRegex + ")?\"(?:\\s+(?:title=\"(.*?)\"))?>(.*?)</a>", Pattern.CASE_INSENSITIVE);

    public static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = pattern.matcher(markdownContent);

        while (matcher.find()) {
            if (URL.inInlineCode(matcher, markdownContent)) {
                continue;
            }
            AnchorLink extractedLink = new AnchorLink();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.setUrl(matcher.group(1));
            extractedLink.title = matcher.group(2);
            extractedLink.anchor = matcher.group(3);
            // e.g., <a href=""> </a>
            if (!extractedLink.getUrlObject().isEmpty()) {
                extractedLinks.add(extractedLink);
            }
        }

        return extractedLinks;
    }
}
