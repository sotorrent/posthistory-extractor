package de.unitrier.st.soposthistory.urls;

import de.unitrier.st.util.Patterns;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class Link {
    String fullMatch;
    String anchor; // the link anchor visible to the user
    String reference; // internal Markdown reference for the link
    String url;
    String title;
    String protocol;
    String completeDomain;
    String rootDomain;
    String path;

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

    public String getProtocol() {
        return protocol;
    }

    public String getCompleteDomain() {
        return completeDomain;
    }

    public String getRootDomain() {
        return rootDomain;
    }

    public String getPath() {
        return path;
    }

    private void extractURLComponents() {
        this.protocol = Patterns.extractProtocol(url);
        this.completeDomain = Patterns.extractCompleteDomain(url);
        this.rootDomain = Patterns.extractRootDomain(completeDomain);
        this.path = Patterns.extractPath(url);
    }

    public static List<Link> extractBare(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher urlMatcher = Patterns.url.matcher(markdownContent);

        while (urlMatcher.find()) {
            Link extractedLink = new Link();
            extractedLink.fullMatch = urlMatcher.group(0).trim();
            extractedLink.url = extractedLink.fullMatch;  // for bare links, the full match is equal to the url match
            if (extractedLink.url.length() > 0) {
                extractedLink.extractURLComponents();
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
                Matcher urlMatcher = Patterns.url.matcher(currentLink.url.trim());
                if (urlMatcher.matches()) {
                    validLinks.add(currentLink);
                }
            }
        }

        for (Link link : validLinks) {
            link.extractURLComponents();
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
