package de.unitrier.st.soposthistory.urls;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Link {
    // for the basic regex, see https://stackoverflow.com/a/6041965, alternative: https://stackoverflow.com/a/29288898
    private static final Pattern urlPattern = Pattern.compile("(?:http|ftp|https)://(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])");
    // pattern to extract domain from URL
    private static final Pattern domainPattern = Pattern.compile("(?i:http|ftp|https)(?:://)([\\w_-]+(?:(?:\\.[\\w_-]+)+))");

    String fullMatch;
    String anchor; // the link anchor visible to the user
    String reference; // internal Markdown reference for the link
    String url;
    String title;
    String domain;

    public String getFullMatch() {
        return fullMatch;
    }

    public String getAnchor() {
        return anchor;
    }

    public String getReference() {
        return reference;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDomain() {
        return domain;
    }

    private void extractDomain() {
        Matcher domainMatcher = domainPattern.matcher(url);
        if (domainMatcher.find()) {
            this.domain = domainMatcher.group(1);
        } else {
            throw new IllegalArgumentException("Extraction of domain failed for URL: " + url);
        }
    }

    public static List<Link> extractBare(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = urlPattern.matcher(markdownContent);

        while (matcher.find()) {
            Link extractedLink = new Link();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.url = matcher.group(0);
            if (extractedLink.url != null && extractedLink.url.length() > 0) {
                extractedLink.extractDomain();
                extractedLinks.add(extractedLink);
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
        Set<String> extractedUrls = extractedLinks.stream().map(Link::getUrl).collect(Collectors.toSet());
        for (Link bareLink : extractedBareLinks) {
            if (!extractedUrls.contains(bareLink.getUrl())) {
                extractedLinks.add(bareLink);
            }
        }

        // validate the extracted links (possible issues include posts 36273118 and 37625877 with "double[][]" and anchor tags where href does not point to a valid URL)
        List<Link> validLinks = new LinkedList<>();
        for (Link currentLink : extractedLinks) {
            if (currentLink.url != null) {
                Matcher urlMatcher = urlPattern.matcher(currentLink.url.trim());
                if (urlMatcher.matches()) {
                    validLinks.add(currentLink);
                }
            }
        }

        for (Link link : validLinks) {
            link.extractDomain();
        }

        return validLinks;
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
                            "[" + currentLink.getAnchor() + "](" + currentLink.getUrl() +
                                    ((currentLink.getTitle() != null) ? " \"" + currentLink.getTitle() + "\"" : "")
                                    + ")"
                    );
                }

                normalizedMarkdownContent = normalizedMarkdownContent.replace(definition, "");
            } else {
                // bare link
                normalizedMarkdownContent = normalizedMarkdownContent.replace(
                        currentLink.getFullMatch(),
                        "<" + currentLink.getUrl() + ">"
                );
            }
        }

        return normalizedMarkdownContent;
    }
}
