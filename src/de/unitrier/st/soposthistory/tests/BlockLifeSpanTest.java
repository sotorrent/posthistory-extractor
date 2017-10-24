package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.util.BlockLifeSpanSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class BlockLifeSpanTest {

    @Test
    void testBlockLifeSpanSnapshotsEqual(){

        // Compare two BlockLifeSpanSnapshots where both uses the constructor with 7 arguments
        BlockLifeSpanSnapshot snapshotDefault = new BlockLifeSpanSnapshot(4711, 42, 1, 0, 0, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentPostId = new BlockLifeSpanSnapshot(4712, 42, 1, 0, 0, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentPostHistoryId = new BlockLifeSpanSnapshot(4711, 43, 1, 0, 0, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentPostBlockTypeId = new BlockLifeSpanSnapshot(4711, 42, 2, 0, 0, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentVersion = new BlockLifeSpanSnapshot(4711, 42, 1, 1, 0, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentLocalId = new BlockLifeSpanSnapshot(4711, 42, 1, 0, 1, 0, 0);
        BlockLifeSpanSnapshot snapshotDifferentPredId = new BlockLifeSpanSnapshot(4711, 42, 1, 0, 0, 1, 0);
        BlockLifeSpanSnapshot snapshotDifferentSuccId = new BlockLifeSpanSnapshot(4711, 42, 1, 0, 0, 0, 1);

        assertThat(snapshotDefault, is(snapshotDefault));
        assertThat(snapshotDefault, is(not(snapshotDifferentPostId)));
        assertThat(snapshotDefault, is(snapshotDifferentPostHistoryId));
        assertThat(snapshotDefault, is(not(snapshotDifferentPostBlockTypeId)));
        assertThat(snapshotDefault, is(not(snapshotDifferentVersion)));
        assertThat(snapshotDefault, is(not(snapshotDifferentLocalId)));
        assertThat(snapshotDefault, is(snapshotDifferentPredId));
        assertThat(snapshotDefault, is(snapshotDifferentSuccId));


        // Compare two BlockLifeSpanSnapshots where both uses the constructor with 4 arguments
        BlockLifeSpanSnapshot snapshotShortDefault = new BlockLifeSpanSnapshot(4711, 42, 0, 0);
        BlockLifeSpanSnapshot snapshotShortDifferentPostId = new BlockLifeSpanSnapshot(4712, 42, 0, 0);
        BlockLifeSpanSnapshot snapshotShortDifferentPostHistoryId = new BlockLifeSpanSnapshot(4711, 43, 0, 0);
        BlockLifeSpanSnapshot snapshotShortDifferentVersion = new BlockLifeSpanSnapshot(4711, 42, 1, 0);
        BlockLifeSpanSnapshot snapshotShortDifferentLocalId = new BlockLifeSpanSnapshot(4711, 42, 0, 1);

        assertThat(snapshotShortDefault, is(snapshotShortDefault));
        assertThat(snapshotShortDefault, is(not(snapshotShortDifferentPostId)));
        assertThat(snapshotShortDefault, is(snapshotShortDifferentPostHistoryId));
        assertThat(snapshotShortDefault, is(not(snapshotShortDifferentVersion)));
        assertThat(snapshotShortDefault, is(not(snapshotShortDifferentLocalId)));


        // Compare two BlockLifeSpanSnapshots where one side uses the constructor with 4 arguments and the other 7 arguments
        assertThat(snapshotDefault, is(snapshotShortDefault));

        assertThat(snapshotDifferentPostId, is(snapshotShortDifferentPostId));
        assertThat(snapshotDifferentPostHistoryId, is(snapshotShortDifferentPostHistoryId));
        assertThat(snapshotDifferentVersion, is(snapshotShortDifferentVersion));
        assertThat(snapshotDifferentLocalId, is(snapshotShortDifferentLocalId));

        assertThat(snapshotDefault, is(not(snapshotShortDifferentPostId)));
        assertThat(snapshotDefault, is(snapshotShortDifferentPostHistoryId));
        assertThat(snapshotDefault, is(not(snapshotShortDifferentVersion)));
        assertThat(snapshotDefault, is(not(snapshotShortDifferentLocalId)));
    }
}
