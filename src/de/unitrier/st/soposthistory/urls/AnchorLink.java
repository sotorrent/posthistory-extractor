package de.unitrier.st.soposthistory.urls;

import java.util.regex.Pattern;

public class AnchorLink extends Link {
    // Example: <a href="http://example.com" title="example">example</a>
    public static final Pattern regex = Pattern.compile("(<a\\s+href\\s*=\\s*\"((?:http|ftp|https):\\/\\/(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-]))?\"(\\s+(title=\"(.*?)\"))?>(.*?)<\\/a>)");
}
