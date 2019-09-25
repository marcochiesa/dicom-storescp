package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StudyProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyProcessor.class);

    @Autowired
    private Config config;
    private Path storageDir;
    private Path incomingDir;
    private Path zipDir;

    @PostConstruct
    private void setup() {
        this.storageDir = Paths.get(config.getStorageDir());
        this.incomingDir = this.storageDir.resolve(Config.INCOMING_DIR);
        this.zipDir = this.storageDir.resolve(Config.ZIP_DIR);
    }

    @Async
    public void process(Path studyDir) {
        if (studyDir == null)
            throw new IllegalArgumentException("null study path");
        Path study = studyDir.getFileName(); //value of study uid
        Path zipFile = studyDir.getParent().resolve(study.toString() + Config.ZIP_EXT);
        // Zip study directory
        try {
            Util.zipDir(studyDir, zipFile);
        } catch (IOException e) {
            LOG.error("unable to zip directory '" + studyDir + "' to '" + zipFile + "'", e);
            return;
        }
        // Create metadata file
        // Copy metadata file to S3
        // Copy zip to S3
        // delete metadata file
        // delete zip file
        try {
            Files.delete(zipFile);
        } catch(IOException e) {
            LOG.error("unable to delete zip file: " + zipFile, e);
        }
        // delete study directory
        try {
            Files.delete(studyDir);
        } catch(IOException e) {
            LOG.error("unable to delete study directory: " + studyDir, e);
        }
    }
}
