package de.unitrier.st.soposthistory.urls;

import java.util.regex.Pattern;

public class AnchorTextAndUrlPair {
    String fullMatch;
    String fullMatch2;
    String anchor;
    String reference;
    String url;
    String title;
    AnchorRefUrlType type;


    public enum AnchorRefUrlType{               // https://stackoverflow.com/editing-help#code
        // ignore the following case because it is not present in SO dump 2016-12-16
        // spanClassWithAnchorTextAndDirectURL, // Here's a <span class="hi">[poorly-named link](http://www.google.com/ "Google")</span>.
        type_markdownLinkBareTags,              // Have you ever seen <http://example.com>?
        type_anchorLink,                        // <a href="http://example.com" title="example">example</a>
        type_markdownLinkInline,                // e.g. Here's an inline link to [Google](http://www.google.com/).
        type_markdownLinkReference_top,         // e.g. Here's a reference-style link to [Google][1].   ...     and later   ...     [1]: http://www.google.com/
        type_markdownLinkReference_bottom,      // e.g. Here's a reference-style link to [Google][1].   ...     and later   ...     [1]: http://www.google.com/
        type_bareURL,                           // I often visit http://example.com.
    }

    public static Pattern getRegexWithEnum(AnchorRefUrlType type){
        switch (type){
            // case spanClassWithAnchorTextAndDirectURL:
            //    return null;
            case type_markdownLinkBareTags:
                return MarkdownLinkBareTags.regex;
            case type_anchorLink:
                return AnchorLink.regex;
            case type_markdownLinkInline:
                return MarkdownLinkInline.regex;
            case type_markdownLinkReference_top:
                return MarkdownLinkReference.regex_top;
            case type_markdownLinkReference_bottom:
                return MarkdownLinkReference.regex_bottom;
            case type_bareURL:
                return Link.regex;

            default:
                return null;
        }
    }


    AnchorTextAndUrlPair(String fullMatch, String anchor, String reference, String url, String title, AnchorRefUrlType anchorRefUrlType) {
        this.fullMatch = fullMatch;
        this.anchor = anchor;
        this.reference = reference;
        this.url = url;
        this.title = title;
        this.type = anchorRefUrlType;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "full match: " + fullMatch + "\n"
                + ((fullMatch2 != null)? "full match bottom: " + fullMatch2 + "\n" : "")
                + "anchor text: " + anchor + "\n"
                + "reference: " + reference + "\n"
                + "URL: " + url + "\n"
                + "Title: " + title + "\n"
                + "Type: " + type + "\n"
                + "\n";
    }

    public AnchorRefUrlType getType() {
        return type;
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

    public String getFullMatch(){
        return fullMatch;
    }
}
