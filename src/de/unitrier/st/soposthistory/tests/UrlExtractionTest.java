package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.urls.*;
import de.unitrier.st.soposthistory.version.PostVersion;
import de.unitrier.st.soposthistory.version.PostVersionList;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.junit.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlExtractionTest {

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

        PostVersionList a_33 = new PostVersionList();
        a_33.readFromCSV("testdata", 33, 2);

        PostVersion version_1 = a_33.getFirst();
        List<Link> extractedUrls = Link.extractAll(version_1.getContent());

        assertEquals(2, extractedUrls.size());

        assertEquals("[reference](http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx)", extractedUrls.get(0).getFullMatch());
        assertEquals("reference", extractedUrls.get(0).getAnchor());
        assertEquals(null, extractedUrls.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.math.truncate.aspx", extractedUrls.get(0).getUrl());
        assertEquals(null, extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkInline.class));

        assertEquals("[Reference.](http://msdn.microsoft.com/en-us/library/system.math.round.aspx)", extractedUrls.get(1).getFullMatch());
        assertEquals("Reference.", extractedUrls.get(1).getAnchor());
        assertEquals(null, extractedUrls.get(1).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.math.round.aspx", extractedUrls.get(1).getUrl());
        assertEquals(null, extractedUrls.get(1).getTitle());
        assertThat(extractedUrls.get(1), instanceOf(MarkdownLinkInline.class));
    }

    @Test
    void testMarkdownLinkReference(){
        /*
        Consider using a [ManualResetEvent][1] to block the main thread at the end of its processing, and call Reset() on it once the timer's processing has finished.
        If this is something that needs to run constantly, consider moving this into a service process instead of a console app.

        [1]: http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx "MSDN Reference"
         */
        PostVersionList a_44 = new PostVersionList();
        a_44.readFromCSV("testdata", 44, 2);

        PostVersion version_1 = a_44.getFirst();
        List<Link> extractedUrls = Link.extractAll(version_1.getContent());

        assertEquals(1, extractedUrls.size());

        assertEquals("[ManualResetEvent][1]\n[1]: http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx \"MSDN Reference\"", extractedUrls.get(0).getFullMatch());
        assertEquals("ManualResetEvent", extractedUrls.get(0).getAnchor());
        assertEquals("1", extractedUrls.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.threading.manualresetevent.aspx", extractedUrls.get(0).getUrl());
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

        PostVersionList a_1629423 = new PostVersionList();
        a_1629423.readFromCSV("testdata", 1629423, 2);

        PostVersion version_1 = a_1629423.getFirst();
        List<Link> extractedUrls = Link.extractAll(version_1.getContent());

        assertEquals(2, extractedUrls.size());

        assertEquals("<a href=\"http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx\">SelectionStart</a>", extractedUrls.get(0).getFullMatch());
        assertEquals("SelectionStart", extractedUrls.get(0).getAnchor());
        assertEquals(null, extractedUrls.get(0).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionstart.aspx", extractedUrls.get(0).getUrl());
        assertEquals(null, extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(AnchorLink.class));

        assertEquals("<a href=\"http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx\">SelectionLength</a>", extractedUrls.get(1).getFullMatch());
        assertEquals("SelectionLength", extractedUrls.get(1).getAnchor());
        assertEquals(null, extractedUrls.get(1).getReference());
        assertEquals("http://msdn.microsoft.com/en-us/library/system.windows.controls.textbox.selectionlength.aspx", extractedUrls.get(1).getUrl());
        assertEquals(null, extractedUrls.get(1).getTitle());
        assertThat(extractedUrls.get(1), instanceOf(AnchorLink.class));
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

        PostVersionList a_52 = new PostVersionList();
        a_52.readFromCSV("testdata", 52, 2);

        PostVersion version_1 = a_52.getFirst();
        List<Link> extractedUrls = Link.extractAll(version_1.getContent());

        assertEquals(2, extractedUrls.size());

        assertEquals("<http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html>", extractedUrls.get(0).getFullMatch());
        assertEquals(null, extractedUrls.get(0).getAnchor());
        assertEquals(null, extractedUrls.get(0).getReference());
        assertEquals("http://www.gskinner.com/blog/archives/2006/06/as3_resource_ma.html", extractedUrls.get(0).getUrl());
        assertEquals(null, extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkAngleBrackets.class));

        assertEquals("<http://www.craftymind.com/2008/04/09/kick-starting-the-garbage-collector-in-actionscript-3-with-air/>", extractedUrls.get(1).getFullMatch());
        assertEquals(null, extractedUrls.get(1).getAnchor());
        assertEquals(null, extractedUrls.get(1).getReference());
        assertEquals("http://www.craftymind.com/2008/04/09/kick-starting-the-garbage-collector-in-actionscript-3-with-air/", extractedUrls.get(1).getUrl());
        assertEquals(null, extractedUrls.get(1).getTitle());
        assertThat(extractedUrls.get(1), instanceOf(MarkdownLinkAngleBrackets.class));
    }

    @Test
    void testLink(){
        /*
        Here is one hack that might work. Isn't clean, but it looks like it might work:

        http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/

        Essentially you just try to update a column that doesn't exist.
         */

        PostVersionList a_49 = new PostVersionList();
        a_49.readFromCSV("testdata", 49, 2);

        PostVersion version_1 = a_49.getFirst();
        List<Link> extractedUrls = Link.extractAll(version_1.getContent());

        assertEquals(1, extractedUrls.size());

        assertEquals("http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/", extractedUrls.get(0).getFullMatch());
        assertEquals(null, extractedUrls.get(0).getAnchor());
        assertEquals(null, extractedUrls.get(0).getReference());
        assertEquals("http://www.brokenbuild.com/blog/2006/08/15/mysql-triggers-how-do-you-abort-an-insert-update-or-delete-with-a-trigger/", extractedUrls.get(0).getUrl());
        assertEquals(null, extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(Link.class));
    }

    @Test
    void testNormalizationOfPostVersionLists(){

        PostVersionList a_33 = new PostVersionList();
        a_33.readFromCSV("testdata", 33, 2);

        PostVersionList a_44 = new PostVersionList();
        a_44.readFromCSV("testdata", 44, 2);

        PostVersionList a_49 = new PostVersionList();
        a_49.readFromCSV("testdata", 49, 2);

        PostVersionList a_52 = new PostVersionList();
        a_52.readFromCSV("testdata", 52, 2);

        PostVersionList a_1629423 = new PostVersionList();
        a_1629423.readFromCSV("testdata", 1629423, 2);


        LinkedList<Link> extractedLinks = new LinkedList<>();

        Link.normalizeLinks(a_33);
        PostVersion version_1_a33 = a_33.getFirst();
        extractedLinks.addAll(Link.extractAll(version_1_a33.getContent()));

        Link.normalizeLinks(a_44);
        PostVersion version_1_a44 = a_44.getFirst();
        extractedLinks.addAll(Link.extractAll(version_1_a44.getContent()));

        Link.normalizeLinks(a_49);
        PostVersion version_1_a49 = a_49.getFirst();
        extractedLinks.addAll(Link.extractAll(version_1_a49.getContent()));

        Link.normalizeLinks(a_52);
        PostVersion version_1_a52 = a_52.getFirst();
        extractedLinks.addAll(Link.extractAll(version_1_a52.getContent()));

        Link.normalizeLinks(a_1629423);
        PostVersion version_1_a1629423 = a_1629423.getFirst();
        extractedLinks.addAll(Link.extractAll(version_1_a1629423.getContent()));


        for(Link link : extractedLinks){
            assertEquals(false, link instanceof MarkdownLinkReference);
        }
    }

    @Test
    void testMarkdownLinkInlineTitle(){
        List<Link> extractedUrls = Link.extractAll("[I'm an inline-style link with title](https://www.google.com \"Google's Homepage\")");

        assertEquals(1, extractedUrls.size());

        assertEquals("[I'm an inline-style link with title](https://www.google.com \"Google's Homepage\")", extractedUrls.get(0).getFullMatch());
        assertEquals("I'm an inline-style link with title", extractedUrls.get(0).getAnchor());
        assertEquals(null, extractedUrls.get(0).getReference());
        assertEquals("https://www.google.com", extractedUrls.get(0).getUrl());
        assertEquals("Google's Homepage", extractedUrls.get(0).getTitle());
        assertThat(extractedUrls.get(0), instanceOf(MarkdownLinkInline.class));
    }
}
