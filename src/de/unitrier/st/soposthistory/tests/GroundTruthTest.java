package de.unitrier.st.soposthistory.tests;

import de.unitrier.st.soposthistory.gt.GroundTruth;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GroundTruthTest {
    @Test
    void testReadFromDirectory() {
        Path testDataDir = Paths.get("testdata", "gt");
        List<GroundTruth> groundTruthList = GroundTruth.readFromDirectory(testDataDir);
        try {
            assertEquals(Files.list(testDataDir).filter(
                    file -> file
                            .getFileName()
                            .toString()
                            .endsWith(".csv"))
                            .count(),
                    groundTruthList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
