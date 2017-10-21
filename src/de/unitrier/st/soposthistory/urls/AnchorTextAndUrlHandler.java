package de.unitrier.st.soposthistory.urls;

import de.unitrier.st.soposthistory.blocks.TextBlockVersion;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;

import java.util.Objects;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnchorTextAndUrlHandler {

    public LinkedList<AnchorTextAndUrlPair> extractAllAnchorsRefsAndURLpairs(String markdown){
        LinkedList<AnchorTextAndUrlPair> anchorTextAndUrlPairs = new LinkedList<>();

        for(int i=0; i< AnchorTextAndUrlPair.AnchorRefUrlType.values().length; i++){
            AnchorTextAndUrlPair.AnchorRefUrlType tmpType = AnchorTextAndUrlPair.AnchorRefUrlType.values()[i];
            Pattern tmpRegex = AnchorTextAndUrlPair.getRegexWithEnum(tmpType);

            assert tmpRegex != null;
            Matcher matcher = tmpRegex.matcher(markdown);
            while(matcher.find()){
                integrateMatchToAnchorTextAndUrlPairs(matcher, tmpType, anchorTextAndUrlPairs);
            }
        }

        mergeMarkdownLinkReferences(anchorTextAndUrlPairs);

        return anchorTextAndUrlPairs;
    }

    private void mergeMarkdownLinkReferences(LinkedList<AnchorTextAndUrlPair> anchorTextAndUrlPairs) {

        // merge if possible
        for (AnchorTextAndUrlPair pairA : anchorTextAndUrlPairs) {
            if(pairA.type == AnchorTextAndUrlPair.AnchorRefUrlType.type_markdownLinkReference_top){
                for(AnchorTextAndUrlPair pairB : anchorTextAndUrlPairs){
                    if(pairB.type == AnchorTextAndUrlPair.AnchorRefUrlType.type_markdownLinkReference_bottom
                            && Objects.equals(pairA.reference, pairB.reference)){
                        pairA.url = pairB.url;
                        pairA.fullMatch2 = pairB.fullMatch2;
                    }
                }
            }
        }

        // delete references with urls if merged, otherwise keep them only to delete invalid markdown link references
        for (int i=0; i<anchorTextAndUrlPairs.size(); i++) {
            AnchorTextAndUrlPair pairA = anchorTextAndUrlPairs.get(i);
            boolean canBeDeleted = false;
            if(pairA.type == AnchorTextAndUrlPair.AnchorRefUrlType.type_markdownLinkReference_bottom){
                for(AnchorTextAndUrlPair pairB : anchorTextAndUrlPairs){
                    if(pairB.type == AnchorTextAndUrlPair.AnchorRefUrlType.type_markdownLinkReference_top
                            && Objects.equals(pairA.reference, pairB.reference)){
                        canBeDeleted = true;
                        break;
                    }
                }

                // transport it to the end of the list so it cannot delete a reference before merged pair does
                anchorTextAndUrlPairs.remove(pairA);
                if(!canBeDeleted){
                    anchorTextAndUrlPairs.add(pairA);
                }
            }
        }

        // remove anchors with references that do not point to have urls they point. Otherwise cases like 36273118 and 37625877 with "double[][]" occuring in text causes false positives as well as real anchor texts with references that do not have references with urls.
        for(int i=anchorTextAndUrlPairs.size()-1; i>=0; i--){
            if(anchorTextAndUrlPairs.get(i).url == null){
                anchorTextAndUrlPairs.remove(i);
            }

        }
    }

    private void integrateMatchToAnchorTextAndUrlPairs(Matcher matcher, AnchorTextAndUrlPair.AnchorRefUrlType type, LinkedList<AnchorTextAndUrlPair> anchorTextAndUrlPairs){

        switch (type){

            case type_anchorLink:
                anchorTextAndUrlPairs.add(
                        new AnchorTextAndUrlPair(
                                matcher.group(),
                                matcher.group(4),
                                null,
                                matcher.group(1),
                                matcher.group(3),
                                type
                        )
                );
                break;


            case type_markdownLinkBareTags:
                anchorTextAndUrlPairs.add(
                        new AnchorTextAndUrlPair(
                                matcher.group(),
                                null,
                                null,
                                matcher.group(1),
                                null,
                                type
                        ));
                break;


            case type_markdownLinkInline:
                anchorTextAndUrlPairs.add(
                        new AnchorTextAndUrlPair(
                                matcher.group(),
                                matcher.group(1),
                                null,
                                matcher.group(2),
                                matcher.group(3),
                                type
                        ));
                break;


            case type_markdownLinkReference_top:

                String fullMatch = null;
                String anchor = null;
                String referenceTop = null;

                try {
                    fullMatch = matcher.group();
                    anchor = matcher.group(1);
                    referenceTop = matcher.group(2);
                }catch (Exception ignored){}

                anchorTextAndUrlPairs.add(new AnchorTextAndUrlPair(
                                fullMatch,
                                anchor,
                                referenceTop,
                                null,
                                null,
                                type
                        )
                );
                anchorTextAndUrlPairs.getLast().fullMatch = fullMatch;

                break;



            case type_markdownLinkReference_bottom:
                String referenceBottom = null;
                String url = null;
                String title = null;

                try{
                    referenceBottom = matcher.group(2);
                    url = matcher.group(3);
                    title = matcher.group(5);
                }catch (Exception ignored){}

                anchorTextAndUrlPairs.add(new AnchorTextAndUrlPair(
                                null,
                                null,
                                referenceBottom,
                                url,
                                title,
                                type
                        )
                );
                anchorTextAndUrlPairs.getLast().fullMatch2 = matcher.group();

                break;


            case type_bareURL:

                for(AnchorTextAndUrlPair anchorTextAndUrlPair : anchorTextAndUrlPairs){
                    if(Objects.equals(anchorTextAndUrlPair.url, matcher.group(0))){
                        return;
                    }
                }
                anchorTextAndUrlPairs.add(
                        new AnchorTextAndUrlPair(
                                matcher.group(),
                                null,
                                null,
                                matcher.group(0),
                                null,
                                type
                        )
                );
                break;
        }
    }

    public static void normalizeURLsInTextBlocksOfAllVersions(PostVersionList postVersionList, AnchorTextAndUrlHandler anchorTextAndUrlHandler){
        if(postVersionList == null)
            return;

        for (PostVersion postVersion : postVersionList) {
            String textBlocksConcatenated = postVersion.getMergedTextBlockContent();
            LinkedList<AnchorTextAndUrlPair> anchorTextAndUrlPairs = anchorTextAndUrlHandler.extractAllAnchorsRefsAndURLpairs(textBlocksConcatenated);

            for(TextBlockVersion textBlock : postVersion.getTextBlocks()){
                String markdownText = textBlock.getContent();
                markdownText = anchorTextAndUrlHandler.normalizeAnchorsRefsAndURLsForApp(markdownText, anchorTextAndUrlPairs);

                if (markdownText.trim().isEmpty()){ // https://stackoverflow.com/a/3745432
                    postVersion.getPostBlocks().remove(textBlock);
                }else{
                    textBlock.setContent(markdownText);
                }
            }
        }
    }

    public String normalizeAnchorsRefsAndURLsForApp(String markdownText, LinkedList<AnchorTextAndUrlPair> anchorTextAndUrlPairs){

        for (AnchorTextAndUrlPair anchorTextAndUrlPair : anchorTextAndUrlPairs) {

            switch (anchorTextAndUrlPair.type){
                case type_markdownLinkInline:
                    // do nothing, this is the normalized form
                    break;

                case type_markdownLinkReference_top:
                    //if(anchorTextAndUrlPair.fullMatch != null) {   // handles e.g. posts 1336419, 12606836
                        if(anchorTextAndUrlPair.anchor.isEmpty()){ // handles e.g. post 42695138
                            markdownText = markdownText.replace(
                                    anchorTextAndUrlPair.fullMatch,
                                    ""
                            );
                        }else {
                            markdownText = markdownText.replace(
                                    anchorTextAndUrlPair.fullMatch,
                                    "[" + anchorTextAndUrlPair.anchor + "](" + anchorTextAndUrlPair.url + ((anchorTextAndUrlPair.title != null) ? " " + anchorTextAndUrlPair.title : "") + ")"
                            );
                        }

                    markdownText = markdownText.replace(
                            anchorTextAndUrlPair.fullMatch2,
                            ""
                    );
                    //}

                    break;

                case type_markdownLinkReference_bottom:

                    markdownText = markdownText.replace(
                            anchorTextAndUrlPair.fullMatch2,
                            ""
                    );

                    break;


                case type_anchorLink:
                    // do nothing, this would be the result after markup
                    break;

                case type_bareURL:
                    markdownText = markdownText.replaceFirst(anchorTextAndUrlPair.fullMatch, "<" + anchorTextAndUrlPair.url + ">");
                    break;

                case type_markdownLinkBareTags:
                    // do nothing, the url will be marked up by commonmark
                    break;
            }
        }

        return markdownText;
    }
}
