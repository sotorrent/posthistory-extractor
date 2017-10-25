package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.util.PostBlockLifeSpanVersion;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

class BlockLifeSpanTest {

    @Test
    void testPostBlockLifeSpanVersionsEqual(){
        // compare two PostBlockLifeSpanVersions
        PostBlockLifeSpanVersion original = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 0, 0);
        PostBlockLifeSpanVersion differentPostId = new PostBlockLifeSpanVersion(4712, 42, 1, 0, 0, 0, 0);
        PostBlockLifeSpanVersion differentPostHistoryId = new PostBlockLifeSpanVersion(4711, 43, 1, 0, 0, 0, 0);
        PostBlockLifeSpanVersion differentPostBlockTypeId = new PostBlockLifeSpanVersion(4711, 42, 2, 0, 0, 0, 0);
        PostBlockLifeSpanVersion differentVersion = new PostBlockLifeSpanVersion(4711, 42, 1, 1, 0, 0, 0);
        PostBlockLifeSpanVersion differentLocalId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 1, 0, 0);
        PostBlockLifeSpanVersion differentPredId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 1, 0);
        PostBlockLifeSpanVersion differentSuccId = new PostBlockLifeSpanVersion(4711, 42, 1, 0, 0, 0, 1);

        assertThat(original, is(original));
        assertThat(original, is(not(differentPostId)));
        assertThat(original, is(differentPostHistoryId));
        assertThat(original, is(not(differentPostBlockTypeId)));
        assertThat(original, is(not(differentVersion)));
        assertThat(original, is(not(differentLocalId)));
        assertThat(original, is(differentPredId));
        assertThat(original, is(differentSuccId));
    }
}
