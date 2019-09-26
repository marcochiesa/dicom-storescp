package org.getmarco.storescp;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcm4che3.data.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Component
public class StudyProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AmazonS3 s3;
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
        Objects.requireNonNull(studyDir,"null study path");
        Path study = studyDir.getFileName(); //value of study uid

        // Create metadata file
        MetaData metaData = null;
        try {
            Attributes attributes = Util.parseDir(studyDir);
            metaData = new MetaData(attributes);
        } catch (IOException e) {
            LOG.error("unable to parse dicom attributes from study directory: " + studyDir);
            return;
        }
        Path metaFile = studyDir.getParent().resolve(study.toString() + Config.TXT_EXT);
        try {
            this.objectMapper.writeValue(metaFile.toFile(), metaData);
        } catch (IOException e) {
            LOG.error("unable to write metadata file: " + metaFile, e);
            return;
        }

        // Zip study directory
        Path zipFile = studyDir.getParent().resolve(study.toString() + Config.ZIP_EXT);
        try {
            Util.zipDir(studyDir, zipFile);
        } catch (IOException e) {
            LOG.error("unable to zip directory '" + studyDir + "' to '" + zipFile + "'", e);
            return;
        }

        // Copy zip to S3
        s3.putObject(
          config.getStorageBucket(),
          Config.FILES_BUCKET_PREFIX + zipFile.getFileName().toString(),
          zipFile.toFile()
        );
        // Copy metadata file to S3
        s3.putObject(
          config.getStorageBucket(),
          Config.METADATA_BUCKET_PREFIX + metaFile.getFileName().toString(),
          metaFile.toFile()
        );

        // delete metadata file
        try {
            Files.delete(metaFile);
        } catch(IOException e) {
            LOG.error("unable to delete metadata file: " + metaFile, e);
        }
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
