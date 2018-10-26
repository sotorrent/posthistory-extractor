package org.sotorrent.posthistoryextractor.urls;

import org.sotorrent.util.LogUtils;
import org.sotorrent.util.URL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class Link {
    public static Logger logger = null;

    String fullMatch;
    String anchor; // the link anchor visible to the user
    String reference; // internal Markdown reference for the link
    String title;
    private URL urlObject;

    static {
        // configure logger
        try {
            logger = LogUtils.getClassLogger(Link.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Link() {}

    public Link(String url) throws MalformedURLException {
        setUrl(url);
    }

    public void setUrl(String url) throws MalformedURLException {
        this.urlObject = new URL(url);
    }

    public String getFullMatch() {
        return fullMatch;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getReference() {
        return reference;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        String linkType = this.getClass().getSimpleName();
        return linkType.equals("Link") ? "BareLink" : linkType;
    }

    public URL getUrlObject() {
        return urlObject;
    }

    public String getUrlString() {
        return urlObject == null ? null : urlObject.getUrlString();
    }

    public String getPosition(String markdownContent) {
        if (this.fullMatch.trim().equals(markdownContent.trim())) {
            return "LinkOnly";
        }

        // for reference-style links use anchor text, for other links full match
        String anchor = this.anchor == null ? this.fullMatch : this.anchor;
        int pos = markdownContent.indexOf(anchor);
        if (pos == -1) {
            throw new IllegalArgumentException("Link not found in Markdown content");
        }

        int partitionSize = markdownContent.length()/3;

        if (pos <= partitionSize) {
            return "Beginning";
        }

        if (pos <= 2*partitionSize) {
            return "Middle";
        }

        return "End";
    }

    public static List<Link> extractBare(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher urlMatcher = URL.urlPattern.matcher(markdownContent);

        while (urlMatcher.find()) {
            if (URL.inInlineCode(urlMatcher, markdownContent)) {
                continue;
            }
            String url = urlMatcher.group(0);
            try {
                Link extractedLink = new Link(url);
                // for bare links, the full match is equal to the matched url
                extractedLink.fullMatch = extractedLink.getUrlString();
                if (extractedLink.fullMatch.trim().length() > 0) {
                    extractedLinks.add(extractedLink);
                }
            } catch (MalformedURLException e) {
                logger.info("Malformed URL: " + url);
            }
        }

        return extractedLinks;
    }

    public static List<Link> extractTyped(String markdownContent) {
        List<Link> extractedLinks = new LinkedList<>();

        // extract markdown links im angle brackets: Have you ever seen <http://example.com>?
        extractedLinks.addAll(MarkdownLinkAngleBrackets.extract(markdownContent));

        // extract inline markdown links: Here's an inline link to [Google](http://www.google.com/)
        extractedLinks.addAll(MarkdownLinkInline.extract(markdownContent));

        // extract referenced markdown links: Here's a reference-style link to [Google][1].   ...     and later   ...     [1]: http://www.google.com/
        extractedLinks.addAll(MarkdownLinkReference.extract(markdownContent));

        // extract anchor links: <a href="http://example.com" title="example">example</a>
        extractedLinks.addAll(AnchorLink.extract(markdownContent));

        // extract bare links: http://www.google.com/
        List<Link> extractedBareLinks = extractBare(markdownContent);
        // only add bare links that have not been matched before
        Set<String> extractedUrls = extractedLinks.stream().map(Link::getUrlString).collect(Collectors.toSet());
        for (Link bareLink : extractedBareLinks) {
            if (!extractedUrls.contains(bareLink.getUrlString())) {
                extractedLinks.add(bareLink);
            }
        }

        return extractedLinks;
    }

    public static String normalizeLinks(String markdownContent, List<Link> extractedLinks) {
        String normalizedMarkdownContent = markdownContent;

        for (Link currentLink : extractedLinks) {
            if (currentLink instanceof MarkdownLinkInline // this is the normalized form
                    || currentLink instanceof AnchorLink // this would be the result after markup
                    || currentLink instanceof MarkdownLinkAngleBrackets) { // this URL will be converted by Commonmark)
                continue;
            }

            if (currentLink instanceof MarkdownLinkReference) {
                String[] usageAndDefinition = currentLink.getFullMatch().split("\n");
                String usage = usageAndDefinition[0];
                String definition = usageAndDefinition[1];

                if (currentLink.getAnchor().isEmpty()) { // handles, e.g., post 42695138
                    normalizedMarkdownContent = normalizedMarkdownContent.replace(usage, "");
                } else {
                    normalizedMarkdownContent = normalizedMarkdownContent.replace(usage,
                            "[" + currentLink.getAnchor() + "](" + currentLink.getUrlString() +
                                    ((currentLink.getTitle() != null) ? " \"" + currentLink.getTitle() + "\"" : "")
                                    + ")"
                    );
                }

                normalizedMarkdownContent = normalizedMarkdownContent.replace(definition, "");
            } else {
                if (MarkdownLinkReference.patternDefinitions.matcher(normalizedMarkdownContent.trim()).matches()) {
                    // MarkdownLinkReference definition without usage (e.g., post 41480290, version 2)
                    normalizedMarkdownContent = "";
                } else {
                    // bare link
                    normalizedMarkdownContent = normalizedMarkdownContent.replace(
                            currentLink.getFullMatch(),
                            "<" + currentLink.getUrlString() + ">"
                    );
                }
            }
        }

        return normalizedMarkdownContent;
    }
}
