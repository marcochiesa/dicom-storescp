package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class StudyProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyProcessor.class);

    private final Path studyDir;

    public StudyProcessor(Path studyDir) {
        if (studyDir == null)
            throw new IllegalArgumentException("null study path");
        this.studyDir = studyDir;
    }

    public void process() {
        // Move study directory to zip directory
        // Zip study directory
        // Create metadata file
        // Copy metadata file to S3
        // Copy zip to S3
        // delete metadata file
        // delete zip file
        // delete study directory?
    }
}
