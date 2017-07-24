package de.unitrier.st.soposthistory.urls;

import java.util.regex.Pattern;

public class Link {
    // for the basic regex, see https://stackoverflow.com/a/6041965, alternative: https://stackoverflow.com/a/29288898
    public static final Pattern regex = Pattern.compile("((?:http|ftp|https)://(?:[\\w_-]+(?:(?:\\.[\\w_-]+)+))(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-]))");
}
