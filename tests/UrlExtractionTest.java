import org.sotorrent.posthistoryextractor.comments.Comments;
import org.sotorrent.posthistoryextractor.history.Posts;
import org.sotorrent.posthistoryextractor.urls.*;
import org.sotorrent.posthistoryextractor.version.PostVersion;
import org.sotorrent.posthistoryextractor.version.PostVersionList;
import org.junit.jupiter.api.Test;
import org.sotorrent.util.URL;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UrlExtractionTest {
    private static Path pathToComments = Paths.get("testdata", "comments");

    @Test
    void testMarkdownLinkInline(){
        /*
        `Math.Floor` rounds down, `Math.Ceiling` rounds up, and `Math.Truncate` rounds towards zero.
        Thus, `Math.Truncate` is like `Math.Floor` for positive numbers, and like `Math.Ceiling` for negative numbers.
        Here's the [reference](http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx).

        For completeness, `Math.Round` rounds to the nearest integer.
        If the number is exactly midway between two integers, then it rounds towards the even one.
        [Reference.](http://msdn.microsoft.com/en-us/library/system.math.round.aspx)
         */

        PostVersionList a_33 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 33, Posts.ANSWER_ID);

        PostVersion version_1 = a_33.getFirst();
        List<Link> extractedLinks = Link.extractTyped(version_1.getContent());

        assertEquals(2, extractedLinks.size());

        assertEquals("[reference](http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx)", extractedLinks.get(0).getFullMatch());
        assertEquals("reference", extractedLinks.get(0).getAnchor());
        assertNull(extractedLinks.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx", extractedLinks.get(0).getUrlString());
        assertEquals("msdn.microsoft.com", extractedLinks.get(0).getUrlObject().getCompleteDomain());
        assertNull(extractedLinks.get(0).getTitle());
        assertThat(extractedLinks.get(0), instanceOf(MarkdownLinkInline.class));

        assertEquals("[Reference.](http://msdn.microsoft.com/en-us/library/system.math.round.aspx)", extractedLinks.get(1).getFullMatch());
        assertEquals("Reference.", extractedLinks.get(1).getAnchor());
        assertNull(extractedLinks.get(1).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.math.round.aspx", extractedLinks.get(1).getUrlString());
        assertEquals("msdn.microsoft.com", extractedLinks.get(1).getUrlObject().getCompleteDomain());
        assertNull(extractedLinks.get(1).getTitle());
        assertThat(extractedLinks.get(1), instanceOf(MarkdownLinkInline.class));

        // malformed URL
        extractedLinks = Link.extractTyped("double[][] double[]() [anchor](http///msdn.microsoft.com/en-us/library/system.math.round.aspx)");
        assertEquals(0, extractedLinks.size());

        // post that could lead to issues (contains, e.g., double[][])
        PostVersionList q_37625877 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 37625877, Posts.QUESTION_ID);
        assertEquals(2, q_37625877.size());
        PostVersion version_2 = q_37625877.get(1);
        extractedLinks = Link.extractTyped(version_2.getContent());
        assertEquals(0, extractedLinks.size());
    }

    @Test
    void testMarkdownLinkReference(){
        /*
        Consider using a [ManualResetEvent][1] to block the main thread at the end of its processing, and call Reset() on it once the timer's processing has finished.
        If this is something that needs to run constantly, consider moving this into a service process instead of a console app.

        [1]: http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx "MSDN Reference"
         */
        PostVersionList a_44 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 44, Posts.ANSWER_ID);

        PostVersion version_1 = a_44.getFirst();
        List<Link> extractedUrls = Link.extractTyped(version_1.getContent());

        assertEquals(1, extractedUrls.size());

        assertEquals("[ManualResetEvent][1]\n[1]: http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx \"MSDN Reference\"", extractedUrls.get(0).getFullMatch());
        assertEquals("ManualResetEvent", extractedUrls.get(0).getAnchor());
        assertEquals("1", extractedUrls.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx", extractedUrls.get(0).getUrlString());
        assertEquals("msdn.microsoft.com", extractedUrls.get(0).getUrlObject().getCompleteDomain());
        assertEquals("MSDN Reference", extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkReference.class));
    }

    @Test
    void testAnchorLink(){
        /*
        Ideally, you would bind TextBox.<a href="http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx">SelectionStart</a> and TextBox.
        <a href="http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx">SelectionLength</a> to values from the slider.
        (Probably via a converter that implements IMultiValueConverer)

        Unfortunately, you can't, because you can only bind Dependency Properties, and SelectionStart and SelectionLength are not dependency properties.

        This means you would have to handle the "ValueChanged" event on the sliders and set the SelectionStart and SelectionLength via the code.

        Disappointing answer - I bet you were hoping for some slick XAML code :-)
         */

        PostVersionList a_1629423 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 1629423, Posts.ANSWER_ID);

        PostVersion version_1 = a_1629423.getFirst();
        List<Link> extractedUrls = Link.extractTyped(version_1.getContent());

        assertEquals(2, extractedUrls.size());

        assertEquals("<a href=\"http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx\">SelectionStart</a>", extractedUrls.get(0).getFullMatch());
        assertEquals("SelectionStart", extractedUrls.get(0).getAnchor());
        assertNull(extractedUrls.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx", extractedUrls.get(0).getUrlString());
        assertEquals("msdn.microsoft.com", extractedUrls.get(0).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(AnchorLink.class));

        assertEquals("<a href=\"http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx\">SelectionLength</a>", extractedUrls.get(1).getFullMatch());
        assertEquals("SelectionLength", extractedUrls.get(1).getAnchor());
        assertNull(extractedUrls.get(1).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx", extractedUrls.get(1).getUrlString());
        assertEquals("msdn.microsoft.com", extractedUrls.get(1).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(1).getTitle());
        assertThat(extractedUrls.get(1), instanceOf(AnchorLink.class));

        // malformed URL
        List<Link> extractedLinks = Link.extractTyped("<a href=\"http///msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx\">SelectionLength</a>");
        assertEquals(0, extractedLinks.size());
    }

    @Test
    void testMarkdownLinkAngleBrackets(){
        /*
        Have a look at this article

        <http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html>

        IANA actionscript programmer, however the feeling I'm getting is that, because the garbage collector might not run when you want it to.

        Hence
        <http://www.craftymind.com/2008/04/09/kick-starting-the-garbage-collector-in-actionscript-3-with-air/>

        So I'd recommend trying out their collection code and see if it helps

            private var gcCount:int;
            private function startGCCycle():void{
    	        gcCount = 0;
    	        addEventListener(Event.ENTER_FRAME, doGC);
            }
            private function doGC(evt:Event):void{
    	        flash.system.System.gc();
            	if(++gcCount > 1){
    	        	removeEventListener(Event.ENTER_FRAME, doGC);
            		setTimeout(lastGC, 40);
    	        }
            }
            private function lastGC():void{
    	        flash.system.System.gc();
            }
         */

        PostVersionList a_52 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 52, Posts.ANSWER_ID);

        PostVersion version_1 = a_52.getFirst();
        List<Link> extractedUrls = Link.extractTyped(version_1.getContent());

        assertEquals(2, extractedUrls.size());

        assertEquals("<http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html>", extractedUrls.get(0).getFullMatch());
        assertNull(extractedUrls.get(0).getAnchor());
        assertNull(extractedUrls.get(0).getReference());
        assertEquals("http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html", extractedUrls.get(0).getUrlString());
        assertEquals("www.gskinner.com", extractedUrls.get(0).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkAngleBrackets.class));

        assertEquals("<http://www.craftymind.com/2008/04/09/kick-starting-the-garbage-collector-in-actionscript-3-with-air/>", extractedUrls.get(1).getFullMatch());
        assertNull(extractedUrls.get(1).getAnchor());
        assertNull(extractedUrls.get(1).getReference());
        assertEquals("http://www.craftymind.com/2008/04/09/kick-starting-the-garbage-collector-in-actionscript-3-with-air/", extractedUrls.get(1).getUrlString());
        assertEquals("www.craftymind.com", extractedUrls.get(1).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(1).getTitle());
        assertThat(extractedUrls.get(1), instanceOf(MarkdownLinkAngleBrackets.class));
    }

    @Test
    void testBareLink(){
        /*
        Here is one hack that might work. Isn't clean, but it looks like it might work:

        http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/

        Essentially you just try to update a column that doesn't exist.
         */

        PostVersionList a_49 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 49, Posts.ANSWER_ID);

        PostVersion version_1 = a_49.getFirst();
        List<Link> extractedUrls = Link.extractTyped(version_1.getContent());

        assertEquals(1, extractedUrls.size());

        assertEquals("http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/", extractedUrls.get(0).getFullMatch());
        assertNull(extractedUrls.get(0).getAnchor());
        assertNull(extractedUrls.get(0).getReference());
        assertEquals("http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/", extractedUrls.get(0).getUrlString());
        assertEquals("www.brokenbuild.com", extractedUrls.get(0).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(Link.class));

        //-----------------------------------

        Matcher urlMatcher;

        urlMatcher = URL.urlPattern.matcher("http://regexpal.com/"); // see method Link.extractTyped
        assertTrue(urlMatcher.matches());

        urlMatcher = URL.urlPattern.matcher("http://blabla/"); // see method Link.extractTyped
        assertFalse(urlMatcher.matches());
    }

    @Test
    void testNormalizationOfPostVersionLists(){
        PostVersionList a_33 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 33, Posts.ANSWER_ID);
        PostVersionList a_44 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 44, Posts.ANSWER_ID);
        PostVersionList a_49 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 49, Posts.ANSWER_ID);
        PostVersionList a_52 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 52, Posts.ANSWER_ID);
        PostVersionList a_1629423 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 1629423, Posts.ANSWER_ID);

        LinkedList<Link> extractedLinks = new LinkedList<>();

        a_33.normalizeLinks();
        PostVersion version_1_a33 = a_33.getFirst();
        extractedLinks.addAll(Link.extractTyped(version_1_a33.getContent()));

        a_44.normalizeLinks();
        PostVersion version_1_a44 = a_44.getFirst();
        extractedLinks.addAll(Link.extractTyped(version_1_a44.getContent()));

        a_49.normalizeLinks();
        PostVersion version_1_a49 = a_49.getFirst();
        extractedLinks.addAll(Link.extractTyped(version_1_a49.getContent()));

        a_52.normalizeLinks();
        PostVersion version_1_a52 = a_52.getFirst();
        extractedLinks.addAll(Link.extractTyped(version_1_a52.getContent()));

        a_1629423.normalizeLinks();
        PostVersion version_1_a1629423 = a_1629423.getFirst();
        extractedLinks.addAll(Link.extractTyped(version_1_a1629423.getContent()));

        for(Link link : extractedLinks){
            assertFalse(link instanceof MarkdownLinkReference);
        }
    }

    @Test
    void testMarkdownLinkInlineTitle(){
        List<Link> extractedUrls = Link.extractTyped("[I'm an inline-style link with title](https://www.google.com \"Google's Homepage\")");

        assertEquals(1, extractedUrls.size());

        assertEquals("[I'm an inline-style link with title](https://www.google.com \"Google's Homepage\")", extractedUrls.get(0).getFullMatch());
        assertEquals("I'm an inline-style link with title", extractedUrls.get(0).getAnchor());
        assertNull(extractedUrls.get(0).getReference());
        assertEquals("https://www.google.com", extractedUrls.get(0).getUrlString());
        assertEquals("Google's Homepage", extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkInline.class));

        extractedUrls = Link.extractTyped("[I'm an inline-style link without title](https://www.google.com)");

        assertEquals(1, extractedUrls.size());

        assertEquals("[I'm an inline-style link without title](https://www.google.com)", extractedUrls.get(0).getFullMatch());
        assertEquals("I'm an inline-style link without title", extractedUrls.get(0).getAnchor());
        assertNull(extractedUrls.get(0).getReference());
        assertEquals("https://www.google.com", extractedUrls.get(0).getUrlString());
        assertEquals("www.google.com", extractedUrls.get(0).getUrlObject().getCompleteDomain());
        assertNull(extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkInline.class));
    }

    @Test
    void testDeletionOfEmptyTextBlocksAfterNormalization () {
        // version 2 should have 4 text blocks and 2 code blocks
        // after normalization, the last block, which contains only a reference and a URL, should be deleted because it's empty
        PostVersionList a_19049539 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 19049539, Posts.ANSWER_ID);
        a_19049539.normalizeLinks();
        PostVersion version_2_a_19049539 = a_19049539.get(1);
        assertEquals(version_2_a_19049539.getTextBlocks().size(), 3);
    }

    @Test
    void testUrlComponentExtraction() {
        testUrlComponents("https://developers.facebook.com/docs/messenger-platform/thread-settings/greeting-text/",
                "https",
                "developers.facebook.com", "facebook.com",
                "docs/messenger-platform/thread-settings/greeting-text");

        testUrlComponents("http://i.stack.imgur.com/Wl2DC.png",
                "http",
                "i.stack.imgur.com", "imgur.com",
                "Wl2DC.png");

        testUrlComponents("http://dev.mysql.com/doc/refman/5.5/en/create-table.html",
                "http",
                "dev.mysql.com", "mysql.com",
                "doc/refman/5.5/en/create-table.html");

        testUrlComponents("http://book.cakephp.org/2.0/en/core-libraries/helpers/html.html#HtmlHelper::image",
                "http",
                "book.cakephp.org", "cakephp.org",
                "2.0/en/core-libraries/helpers/html.html");

        testUrlComponents("https://webcache.googleusercontent.com/search?q=cache:F1YnhmHMSkwJ:https://www.w3.org/Addressing/URL/uri-spec.ps%20&cd=2&hl=en&ct=clnk&gl=uk",
                "https",
                "webcache.googleusercontent.com", "googleusercontent.com",
                "search");

        testUrlComponents("http://developer.android.com/reference/android/view/ViewGroup.html#indexOfChild%28android.view.View%29",
                "http",
                "developer.android.com", "android.com",
                "reference/android/view/ViewGroup.html");

        testUrlComponents("ftp://ftp.linux-magazine.com/pub/listings/magazine/185/ELKstack/configfiles/etc_logstash/conf.d/5003-postfix-filter.conf",
                "ftp",
                "ftp.linux-magazine.com", "linux-magazine.com",
                "pub/listings/magazine/185/ELKstack/configfiles/etc_logstash/conf.d/5003-postfix-filter.conf");
    }

    private void testUrlComponents(String url, String expectedProtocol,
                         String expectedCompleteDomain, String expectedRootDomain,
                         String expectedPath) {
        Link link = Link.extractBare(url).get(0);
        assertEquals(expectedProtocol, link.getUrlObject().getProtocol());
        assertEquals(expectedCompleteDomain, link.getUrlObject().getCompleteDomain());
        assertEquals(expectedRootDomain, link.getUrlObject().getRootDomain());
        assertEquals(expectedPath, link.getUrlObject().getPath());
    }

    @Test
    void testDoctypeUrl() {
        String inputString = "DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";

        Matcher urlMatcher = URL.urlPattern.matcher(inputString);
        assertTrue(urlMatcher.find());
        Link link = Link.extractBare(inputString).get(0);
        assertEquals(urlMatcher.group(0), link.getUrlString());

        assertEquals("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd", link.getUrlString());
        assertEquals("http", link.getUrlObject().getProtocol());
        assertEquals("www.w3.org", link.getUrlObject().getCompleteDomain());
        assertEquals("w3.org", link.getUrlObject().getRootDomain());
        assertEquals("TR/xhtml11/DTD/xhtml11.dtd", link.getUrlObject().getPath());
    }

    @Test
    void testLinkTypeAndAnchorExtraction() {
        PostVersionList postVersionList;
        Link extractedLink;

        // AnchorLink
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 1629423, Posts.ANSWER_ID);
        extractedLink = Link.extractTyped(postVersionList.getFirst().getContent()).get(0);
        assertEquals("<a href=\"http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx\">SelectionStart</a>", extractedLink.getFullMatch());
        assertEquals("AnchorLink", extractedLink.getType());
        assertEquals("SelectionStart", extractedLink.getAnchor());

        // BareLink
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 49, Posts.ANSWER_ID);
        extractedLink = Link.extractTyped(postVersionList.getFirst().getContent()).get(0);
        assertEquals("http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/", extractedLink.getFullMatch());
        assertEquals("BareLink", extractedLink.getType());
        assertNull(extractedLink.getAnchor());

        // MarkdownLinkAngleBrackets
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 52, Posts.ANSWER_ID);
        extractedLink = Link.extractTyped(postVersionList.getFirst().getContent()).get(0);
        assertEquals("<http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html>", extractedLink.getFullMatch());
        assertEquals("MarkdownLinkAngleBrackets", extractedLink.getType());
        assertNull(extractedLink.getAnchor());

        // MarkdownLinkInline
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 33, Posts.ANSWER_ID);
        extractedLink = Link.extractTyped(postVersionList.getFirst().getContent()).get(0);
        assertEquals("[reference](http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx)",extractedLink.getFullMatch());
        assertEquals("MarkdownLinkInline", extractedLink.getType());
        assertEquals("reference", extractedLink.getAnchor());

        // MarkdownLinkReference
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 44, Posts.ANSWER_ID);
        extractedLink = Link.extractTyped(postVersionList.getFirst().getContent()).get(0);
        assertEquals("[ManualResetEvent][1]\n[1]: http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx \"MSDN Reference\"", extractedLink.getFullMatch());
        assertEquals("MarkdownLinkReference", extractedLink.getType());
        assertEquals("ManualResetEvent", extractedLink.getAnchor());
    }

    @Test
    void testFragmentsAndPathDomain() {
        Link link;

        try {
            link = new Link("http://en.wikipedia.org/wiki/regular_expression#syntax");
            assertEquals("wiki/regular_expression", link.getUrlObject().getPath());
            assertEquals("syntax", link.getUrlObject().getFragmentIdentifier());

            link = new Link("http://www.regular-expressions.info/lookaround.html");
            assertEquals("lookaround.html", link.getUrlObject().getPath());
            assertNull(link.getUrlObject().getFragmentIdentifier());

            link = new Link("https://docs.oracle.com/");
            assertNull(link.getUrlObject().getPath());
            assertNull(link.getUrlObject().getFragmentIdentifier());

            link = new Link("https://docs.oracle.com");
            assertNull(link.getUrlObject().getPath());
            assertNull(link.getUrlObject().getFragmentIdentifier());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLinkOnlyComment() {
        Comments comment = Comments.readFromCSV(pathToComments, 62304497);
        comment.extractUrls();
        List<CommentUrl> extractedUrls = comment.getExtractedUrls();
        assertEquals(1, extractedUrls.size());
        CommentUrl commentUrl = extractedUrls.get(0);
        assertEquals("BareLink", commentUrl.getLinkType());
        assertEquals("LinkOnly", commentUrl.getLinkPosition());
    }

    @Test
    void testBareLinkInComment() {
        Comments comment = Comments.readFromCSV(pathToComments, 16570458);
        comment.extractUrls();
        List<CommentUrl> extractedUrls = comment.getExtractedUrls();
        assertEquals(1, extractedUrls.size());
        CommentUrl commentUrl = extractedUrls.get(0);
        assertEquals("BareLink", commentUrl.getLinkType());
    }

    @Test
    void testLinkPosition() {
        PostVersionList postVersionList;
        PostVersion version;
        Link extractedLink;

        // link at the beginning of a post (pointing to duplicate post)
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 12954660, Posts.QUESTION_ID);
        version = postVersionList.getLast();
        extractedLink = Link.extractTyped(version.getContent()).get(0);
        assertEquals("Beginning", extractedLink.getPosition(version.getContent()));

        // link at the end of a post
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 28153330, Posts.ANSWER_ID);
        version = postVersionList.getLast();
        extractedLink = Link.extractTyped(version.getContent()).get(0);
        assertEquals("End", extractedLink.getPosition(version.getContent()));

        // link in the middle of a post
        postVersionList = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 14928352, Posts.ANSWER_ID);
        version = postVersionList.getLast();
        extractedLink = Link.extractTyped(version.getContent()).get(1);
        assertEquals("Middle", extractedLink.getPosition(version.getContent()));
    }

    @Test
    void testPunctuationAndWhitespaceRemoval() {
        Link link;

        try {
            link = new Link("http://weblogs.sqlteam.com/.");
            assertNull(link.getUrlObject().getPath());

            link = new Link("https://khaccounts.net//");
            assertNull(link.getUrlObject().getPath());

            link = new Link("http://www.websitetest.com/&#xA");
            assertNull(link.getUrlObject().getPath());

            link = new Link("http://www.websitetest.com/&#xD;");
            assertNull(link.getUrlObject().getPath());

            link = new Link("http://jquery.com/:");
            assertNull(link.getUrlObject().getPath());

            new Link(null); // should not throw a NullPointerException

            link = new Link("http://stackoverflow.com/questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224.");
            assertEquals("http://stackoverflow.com/questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224", link.getUrlString());
            assertEquals("questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224", link.getUrlObject().getPath());

            link = new Link("http://stackoverflow.com/questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224/.");
            assertEquals("http://stackoverflow.com/questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224/", link.getUrlString());
            assertEquals("questions/31423480/memsql-leaf-down-on-single-server-cluster/35270224", link.getUrlObject().getPath());

            link = new Link("http://www.mediawiki.org/wiki/Manual:");
            assertEquals("http://www.mediawiki.org/wiki/Manual", link.getUrlString());
            assertEquals("wiki/Manual", link.getUrlObject().getPath());

            link = new Link("http://www.sybase.com/detail?id=1056497,");
            assertEquals("http://www.sybase.com/detail?id=1056497", link.getUrlString());
            assertEquals("detail", link.getUrlObject().getPath());

            link = new Link("http://stackoverflow.com/questions/7341017/spritesheet-programmatically-cutting-best-practices..,&#xA.&#xD,");
            assertEquals("http://stackoverflow.com/questions/7341017/spritesheet-programmatically-cutting-best-practices", link.getUrlString());
            assertEquals("questions/7341017/spritesheet-programmatically-cutting-best-practices", link.getUrlObject().getPath());
        } catch (MalformedURLException e) {
            Link.logger.warning(e.getMessage());
        }
    }

    @Test
    void testSpecialUrls() {
        Link link;

        link = Link.extractBare("https://en.wikipedia.org/wiki/Glob_(programming)").get(0);
        assertEquals("https://en.wikipedia.org/wiki/Glob_(programming)", link.getUrlString());
        assertEquals("wiki/Glob_(programming)", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("http://en.wikipedia.org/wiki/Magic_number_%28programming%29").get(0);
        assertEquals("http://en.wikipedia.org/wiki/Magic_number_%28programming%29", link.getUrlString());
        assertEquals("wiki/Magic_number_%28programming%29", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://groups.google.com/forum/?fromgroups=#!topic/android-platform/sR6I2ldCxwU").get(0);
        assertEquals("https://groups.google.com/forum/?fromgroups=#!topic/android-platform/sR6I2ldCxwU", link.getUrlString());
        assertEquals("forum", link.getUrlObject().getPath());
        assertEquals("fromgroups=", link.getUrlObject().getQuery());
        assertEquals("!topic/android-platform/sR6I2ldCxwU", link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://groups.google.com/forum/?fromgroups#").get(0);
        assertEquals("https://groups.google.com/forum/?fromgroups", link.getUrlString());
        assertEquals("forum", link.getUrlObject().getPath());
        assertEquals("fromgroups", link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://groups.google.com/forum/?").get(0);
        assertEquals("https://groups.google.com/forum/?", link.getUrlString());
        assertEquals("forum", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://developers.google.com/android/reference/com/google/android/gms/location/places/PlaceDetectionClient.html#reportDeviceAtPlace(com.google.android.gms.location.places.PlaceReport)").get(0);
        assertEquals("https://developers.google.com/android/reference/com/google/android/gms/location/places/PlaceDetectionClient.html#reportDeviceAtPlace(com.google.android.gms.location.places.PlaceReport)", link.getUrlString());
        assertEquals("android/reference/com/google/android/gms/location/places/PlaceDetectionClient.html", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertEquals("reportDeviceAtPlace(com.google.android.gms.location.places.PlaceReport)", link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://www.simplifiedcoding.net/android-volley-post-request-tutorial/......").get(0);
        assertEquals("https://www.simplifiedcoding.net/android-volley-post-request-tutorial/", link.getUrlString());
        assertEquals("android-volley-post-request-tutorial", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://www.google.com/webhp?#q=firebase+role+based+security").get(0);
        assertEquals("https://www.google.com/webhp?#q=firebase+role+based+security", link.getUrlString());
        assertEquals("webhp", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertEquals("q=firebase+role+based+security", link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("http://wiki.eclipse.org/FAQ_How_do_I_run_Eclipse?#Find_the_JVM").get(0);
        assertEquals("http://wiki.eclipse.org/FAQ_How_do_I_run_Eclipse?#Find_the_JVM", link.getUrlString());
        assertEquals("FAQ_How_do_I_run_Eclipse", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertEquals("Find_the_JVM", link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://developers.google.com/appengine/docs/java/datastore/entities?#Java_Properties_and_value_types").get(0);
        assertEquals("https://developers.google.com/appengine/docs/java/datastore/entities?#Java_Properties_and_value_types", link.getUrlString());
        assertEquals("appengine/docs/java/datastore/entities", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertEquals("Java_Properties_and_value_types", link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns.html?#DURATION").get(0);
        assertEquals("https://developer.android.com/reference/android/provider/CalendarContract.EventsColumns.html?#DURATION", link.getUrlString());
        assertEquals("android.com", link.getUrlObject().getRootDomain());
        assertEquals("developer.android.com", link.getUrlObject().getCompleteDomain());
        assertEquals("reference/android/provider/CalendarContract.EventsColumns.html", link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertEquals("DURATION", link.getUrlObject().getFragmentIdentifier());
    }

    @Test
    void testQueryExtraction() {
        Link link;

        try {
            link = new Link("http://code.google.com/p/android/issues/detail?id=4611");
            assertEquals("p/android/issues/detail", link.getUrlObject().getPath());
            assertEquals("id=4611", link.getUrlObject().getQuery());

            link = new Link("https://code.google.com/p/android/issues/detail?id=78471&colspec=id%20type%20status%20owner%20summary%20stars");
            assertEquals("p/android/issues/detail", link.getUrlObject().getPath());
            assertEquals("id=78471&colspec=id%20type%20status%20owner%20summary%20stars", link.getUrlObject().getQuery());

            link = new Link("https://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(java.lang.String,%20long,%20float,%20android.app.PendingIntent)");
            assertEquals("https://developer.android.com/reference/android/location/LocationManager.html#requestLocationUpdates(java.lang.String,%20long,%20float,%20android.app.PendingIntent)", link.getUrlObject().getUrlString());
            assertEquals("reference/android/location/LocationManager.html", link.getUrlObject().getPath());
            assertNull(link.getUrlObject().getQuery());
            assertEquals("requestLocationUpdates(java.lang.String,%20long,%20float,%20android.app.PendingIntent)", link.getUrlObject().getFragmentIdentifier());

            link = new Link("http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/methods/HttpGet.html#HttpGet(java.net.URI)");
            assertEquals("http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/methods/HttpGet.html#HttpGet(java.net.URI)", link.getUrlObject().getUrlString());
            assertEquals("httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/methods/HttpGet.html", link.getUrlObject().getPath());
            assertNull(link.getUrlObject().getQuery());
            assertEquals("HttpGet(java.net.URI)", link.getUrlObject().getFragmentIdentifier());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testExcludeLinksInInlineCode() {
        List<Link> extractedUrls;

        // bare
        extractedUrls = Link.extractTyped("`http://www.w3.org/2001/XMLSchema#float`");
        assertEquals(0, extractedUrls.size());

        // anchor link
        extractedUrls = Link.extractTyped("`<a href=\"http://example.com\" title=\"example\">example</a>`");
        assertEquals(0, extractedUrls.size());

        // angle brackets
        extractedUrls = Link.extractTyped("`<http://www.w3.org/2001/XMLSchema#float>`");
        assertEquals(0, extractedUrls.size());

        // inline link
        extractedUrls = Link.extractTyped("`[Google](http://www.google.com/)`");
        assertEquals(0, extractedUrls.size());

        // reference-style links do not need to be considered here

        /*  ...
            `^^<http://www.w3.org/2001/XMLSchema#float>`
            ...
         */
        PostVersionList a_38542344 = PostVersionList.readFromCSV(PostVersionHistoryTest.pathToPostVersionLists, 38542344, Posts.ANSWER_ID);

        PostVersion version_1 = a_38542344.getFirst();
        extractedUrls = Link.extractTyped(version_1.getContent());
        assertEquals(0, extractedUrls.size());
    }

    @Test
    void testMalformedURLs() {
        Link link;

        link = Link.extractBare("https://example.com%").get(0);
        assertEquals("https://example.com", link.getUrlString());
        assertEquals("example.com", link.getUrlObject().getRootDomain());
        assertEquals("example.com", link.getUrlObject().getCompleteDomain());
        assertNull(link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        link = Link.extractBare("http://mywebaddress.com%2ftransid=123").get(0);
        assertEquals("http://mywebaddress.com", link.getUrlString());
        assertEquals("mywebaddress.com", link.getUrlObject().getRootDomain());
        assertEquals("mywebaddress.com", link.getUrlObject().getCompleteDomain());
        assertNull(link.getUrlObject().getPath());
        assertNull(link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());

        // this URL should not be treated as malformed
        link = Link.extractBare("http://stackoverflow.com/questions/43810934/android-killing-background-acitivities/43811128?noredirect=1#").get(0);
        assertEquals("http://stackoverflow.com/questions/43810934/android-killing-background-acitivities/43811128?noredirect=1", link.getUrlString());
        assertEquals("stackoverflow.com", link.getUrlObject().getRootDomain());
        assertEquals("stackoverflow.com", link.getUrlObject().getCompleteDomain());
        assertEquals("questions/43810934/android-killing-background-acitivities/43811128", link.getUrlObject().getPath());
        assertEquals("noredirect=1", link.getUrlObject().getQuery());
        assertNull(link.getUrlObject().getFragmentIdentifier());
        boolean exceptionThrown = false;
        try {
            link = new Link("http://stackoverflow.com/questions/43810934/android-killing-background-acitivities/43811128?noredirect=1#");
            assertEquals("http://stackoverflow.com/questions/43810934/android-killing-background-acitivities/43811128?noredirect=1", link.getUrlString());
        } catch (MalformedURLException e) {
            exceptionThrown = true;
        }
        assertFalse(exceptionThrown);

        List<Link> extractedLinks;
        extractedLinks = Link.extractTyped("http://www.rolfje..com/2008/11/04/transporting-oracle-chars-over-a-dblink/");
        assertEquals(0, extractedLinks.size());
    }

    @Test
    void testTextBlockDefinitionWithoutUsage() {
        // taken from post 41480290, version 2
        String content = "   [1]: http://androidbash.com/firebase-push-notification-android/   ";
        List<Link> extractedLinks = Link.extractTyped(content);
        String normalizedContent = Link.normalizeLinks(content, extractedLinks);
        assertEquals(0, normalizedContent.trim().length());
    }
}
