package org.getmarco.storescp;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storescp")
public class Config {
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    public static final String PART_EXT = ".part";
    public static final String DCM_EXT = ".dcm";
    public static final String ZIP_EXT = ".zip";
    public static final String TXT_EXT = ".txt";
    public static final String INCOMING_DIR = "incoming";
    public static final String ZIP_DIR = "zip";
    public static final String DICOMDIR = "DICOMDIR";
    public static final String METADATA_BUCKET_PREFIX = "metadata/";
    public static final String FILES_BUCKET_PREFIX = "files/";
    // Default value 10 minutes if AE Title pair not configured (milliseconds)
    // Study wait time is used as a timeout value to determine if all the files in a study have been received.
    // DICOM transfers don't indicate how many files are in a study, so have to wait until they stop coming.
    public static final int DEFAULT_STUDY_WAIT_TIME = 600000;

    private String deviceName;
    private String storageDir;
    private String storageBucket;
    private String storageBucketRegion;
    private int port;
    private boolean clearStorageDirectoryOnStart;
    private Map<String, Map<String, Integer>> aetitlePairs;

    public Integer getStudyWaitTime(String callingAET, String calledAET) {
        Objects.requireNonNull(calledAET, "null called AE Title");
        Objects.requireNonNull(callingAET, "null calling AE Title");
        return Optional.ofNullable(this.getAetitlePairs().get(calledAET))
          .map(x -> x.get(callingAET)).orElse(DEFAULT_STUDY_WAIT_TIME);
    }

    public boolean isAcceptedLocalAetitle(String calledAET) {
        return this.getAetitlePairs().get(calledAET) != null;
    }

    public boolean hasAetitlePair(String callingAET, String calledAET) {
        return Optional.ofNullable(this.getAetitlePairs().get(calledAET)).map(x -> x.containsKey(callingAET)).orElse(false);
    }

    public Path getStorageDirPath() {
        return Paths.get(getStorageDir());
    }

    public Path getIncomingDirPath() {
        return this.getStorageDirPath().resolve(INCOMING_DIR);
    }

    public Path getZipDirPath() {
        return this.getStorageDirPath().resolve(ZIP_DIR);
    }

    public boolean isCalledAETDir(Path path) {
        if (!Files.isDirectory(path))
            return false;
        try {
            if (Files.isSameFile(this.getIncomingDirPath(), path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path '" + path + "' to incoming directory", e);
            return false;
        }
        try {
            if (Files.isSameFile(this.getZipDirPath(), path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path '" + path + "' to zip directory", e);
            return false;
        }
        return true;
    }
}
