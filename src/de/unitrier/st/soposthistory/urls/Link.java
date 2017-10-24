package de.unitrier.st.soposthistory.urls;

import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Link {
    // for the basic regex, see https://stackoverflow.com/a/6041965, alternative: https://stackoverflow.com/a/29288898
    private static final Pattern regex = Pattern.compile("(?:http|ftp|https):\\/\\/(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])");

    String fullMatch;
    String anchor; // the link anchor visible to the user
    String reference; // internal Markdown reference for the link
    String url;
    String title;

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

    public static List<Link> extract(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();
        Matcher matcher = regex.matcher(markdownContent);

        while (matcher.find()) {
            Link extractedLink = new Link();
            extractedLink.fullMatch = matcher.group(0);
            extractedLink.url = matcher.group(0);
            extractedLinks.add(extractedLink);
        }

        return extractedLinks;
    }

    public static LinkedList<Link> extractAll(String markdownContent) {
        LinkedList<Link> extractedLinks = new LinkedList<>();

        // extract markdown links im angle brackets: Have you ever seen <http://example.com>?
        extractedLinks.addAll(MarkdownLinkAngleBrackets.extract(markdownContent));

        // extract inline markdown links: Here's an inline link to [Google](http://www.google.com/)
        extractedLinks.addAll(MarkdownLinkInline.extract(markdownContent));

        // extract referenced markdown links: Here's a reference-style link to [Google][1].   ...     and later   ...     [1]: http://www.google.com/
        extractedLinks.addAll(MarkdownLinkReference.extract(markdownContent));

        // extract anchor links: <a href="http://example.com" title="example">example</a>
        extractedLinks.addAll(AnchorLink.extract(markdownContent));

        // extract bare links: http://www.google.com/
        List<Link> extractedBareLinks = extract(markdownContent);
        // only add bare links that have not been matched before
        Set<String> extractedUrls = extractedLinks.stream().map(Link::getUrl).collect(Collectors.toSet());
        for (Link bareLink : extractedBareLinks) {
            if (!extractedUrls.contains(bareLink.getUrl())) {
                extractedLinks.add(bareLink);
            }
        }

        // validate the extracted links (possible issues include posts 36273118 and 37625877 with "double[][]" and anchor tags where href does not point to a valid URL)
        LinkedList<Link> validLinks = new LinkedList<>();
        for (Link currentLink : extractedLinks) {
            Matcher urlMatcher = regex.matcher(currentLink.url.trim());
            if (urlMatcher.matches()) {
                validLinks.add(currentLink);
            }
        }

        return validLinks;
    }

    public static void normalizeLinks(PostVersionList postVersionList) {
        if (postVersionList == null) {
            return;
        }

        for (PostVersion postVersion : postVersionList) {
            String mergedTextBlocks = postVersion.getMergedTextBlockContent();
            List<Link> extractedLinks = extractAll(mergedTextBlocks);

            for (TextBlockVersion currentTextBlock : postVersion.getTextBlocks()) {
                String normalizedMarkdownContent = normalizeLinks(currentTextBlock.getContent(), extractedLinks);

                if (normalizedMarkdownContent.trim().isEmpty()) { // handles, e.g., post 3745432
                    postVersion.getPostBlocks().remove(currentTextBlock);
                } else {
                    currentTextBlock.setContent(normalizedMarkdownContent);
                }
            }
        }
    }

    private static String normalizeLinks(String markdownContent, List<Link> extractedLinks) {

        // TODO: replace vs. replaceFirst?

        String normalizedMarkdownContent = markdownContent;

        for (Link currrentLink : extractedLinks) {
            if (currrentLink instanceof MarkdownLinkInline // this is the normalized form
                    || currrentLink instanceof AnchorLink // this would be the result after markup
                    || currrentLink instanceof MarkdownLinkAngleBrackets) { // this URL will be converted by Commonmark)
                continue;
            }

            if (currrentLink instanceof MarkdownLinkReference) {
                String[] usageAndDefinition = currrentLink.getFullMatch().split("\n");
                String usage = usageAndDefinition[0];
                String definition = usageAndDefinition[1];

                if (currrentLink.getAnchor().isEmpty()) { // handles, e.g., post 42695138
                    normalizedMarkdownContent = normalizedMarkdownContent.replace(usage, "");
                } else {
                    normalizedMarkdownContent = normalizedMarkdownContent.replace(usage,
                            "[" + currrentLink.getAnchor() + "](" + currrentLink.getUrl() +
                                    ((currrentLink.getTitle() != null) ? " \"" + currrentLink.getTitle() + "\"" : "")
                                    + ")"
                    );
                }

                normalizedMarkdownContent = normalizedMarkdownContent.replace(definition, "");
            } else {
                // bare link
                normalizedMarkdownContent = normalizedMarkdownContent.replaceFirst(
                        Pattern.quote(currrentLink.getFullMatch()),
                        "<" + currrentLink.getUrl() + ">"
                );
            }
        }

        return normalizedMarkdownContent;
    }
}
