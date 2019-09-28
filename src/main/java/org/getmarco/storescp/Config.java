package org.getmarco.storescp;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storescp")
public class Config {
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
}
