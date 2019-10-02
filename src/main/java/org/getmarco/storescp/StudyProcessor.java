package org.getmarco.storescp;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dcm4che3.data.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * This class processes 'complete' dicom studies that have been received. This
 * will include creating a zip archive with the dicom files for the study,
 * creating a metadata file describing the study. uploading the zip archive and
 * metadata file to S3, and finally deleting the study (zip archive, metadata
 * file, original study directory).
 */
@Component
public class StudyProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(StudyProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AmazonS3 s3;
    @Autowired
    private Config config;

    @Async
    public void process(Path studyDir) {
        Objects.requireNonNull(studyDir,"null study path");
        Path studyDirName = studyDir.getFileName(); //uuid value
        Path metaFile = studyDir.getParent().resolve(studyDirName.toString() + Config.TXT_EXT);
        Path zipFile = studyDir.getParent().resolve(studyDirName.toString() + Config.ZIP_EXT);

        // Create metadata file
        MetaData metaData = null;
        try {
            Attributes attributes = Util.parseDir(studyDir);
            metaData = new MetaData(attributes);
        } catch (IOException e) {
            LOG.error("unable to parse dicom attributes from study directory: " + studyDir);
            return;
        }
        try {
            this.objectMapper.writeValue(metaFile.toFile(), metaData);
        } catch (IOException e) {
            LOG.error("unable to write metadata file: " + metaFile, e);
            return;
        }

        // Zip study directory
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
            FileSystemUtils.deleteRecursively(studyDir);
        } catch(IOException e) {
            LOG.error("unable to delete study directory: " + studyDir, e);
        }
    }
}
