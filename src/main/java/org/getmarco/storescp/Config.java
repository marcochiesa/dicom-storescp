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

    // File extension for writing temporary incoming files
    public static final String PART_EXT = ".part";
    // File extension for transferred dicom files
    public static final String DCM_EXT = ".dcm";
    // File extension for created zip archives
    public static final String ZIP_EXT = ".zip";
    // File extension for created study metadata files
    public static final String TXT_EXT = ".txt";
    // Temporary incoming file transfer storage area
    public static final String INCOMING_DIR = "incoming";
    // Temporary work area for processing completed studies (create zip archive
    // with all dicom files and create study metadata)
    public static final String ZIP_DIR = "zip";
    // S3 object 'directory' prefix when uploading metadata about dicom study
    public static final String METADATA_BUCKET_PREFIX = "metadata/";
    // S3 object 'directory' prefix when uploading dicom file archive
    public static final String FILES_BUCKET_PREFIX = "files/";
    // Default value 15 minutes if AE Title pair not configured (milliseconds)
    // Study wait time is used as a timeout value to determine if all the files
    // in a study have been received. DICOM transfers don't indicate how many
    // files are in a study, so have to wait until they stop coming.
    public static final int DEFAULT_STUDY_WAIT_TIME = 900000;

    // Identifier for this dicom endpoint
    private String deviceName;
    // Filesystem workspace for this storage service class provider (SCP) for:
    // - writing temporary files for incoming transfers
    // - gathering dicom files from incoming transfers into a complete study
    // - workspace for creating a zip archive of the study's dicom files
    // - workspace for creating a metadata file describing the study
    private String storageDir;
    // S3 bucket for uploading completed studies
    private String storageBucket;
    // AWS region for the upload S3 bucket
    private String storageBucketRegion;
    // The port to listen on for incoming store requests
    private int port;
    // Whether to delete (recursively) the contents of the filesystem workspace
    // on application startup. It's an easy way to cleanup the effective
    // 'application state' stored in the filesystem, but any transfers that had
    // been in-progress will need to be restarted.
    private boolean clearStorageDirectoryOnStart;
    // Configuration for the Application Entity Titles on which this
    // application will accepted incoming storage requests. Also, specifies the
    // AE Titles from which storage requests will be accepted. Additionally,
    // for each combination, specifies how long to wait before assuming no more
    // dicom files are coming to complete a particular study. The last aspect
    // is necessary since dicom transfers don't include information about how
    // many files will be sent.
    private Map<String, Map<String, Integer>> aetitlePairs;

    /**
     * For each combination of calling/called AE Titles, this method returns
     * the amount of time to wait after receiving the previous dicom file for
     * a particular study before assuming that no more files for that study
     * will be coming, and that it is appropriate to assume the study is
     * complete (and process it). Falls back to a long default value in case no
     * match found.
     * @param callingAET the remote AE Title making the storage request
     * @param calledAET the local AE Title to which the storage request was made
     * @return the wait time in milliseconds for the given combination
     */
    public Integer getStudyWaitTime(String callingAET, String calledAET) {
        Objects.requireNonNull(calledAET, "null called AE Title");
        Objects.requireNonNull(callingAET, "null calling AE Title");
        return Optional.ofNullable(this.getAetitlePairs().get(calledAET))
          .map(x -> x.get(callingAET)).orElse(DEFAULT_STUDY_WAIT_TIME);
    }

    /**
     * Check whether the application is configured to accept storage requests
     * on the give AE Title
     * @param calledAET the local AE Title to which the storage request was made
     * @return whether to accept a storage request to the given AE Title
     */
    public boolean isAcceptedLocalAetitle(String calledAET) {
        return this.getAetitlePairs().get(calledAET) != null;
    }

    /**
     * Check whether the application is configured for a given combination of
     * calling/called AE Titles.
     * @param callingAET the remote AE Title making the storage request
     * @param calledAET the local AE Title to which the storage request was made
     * @return whether the given AE Title combination is configured as acceptable
     */
    public boolean hasAetitlePair(String callingAET, String calledAET) {
        return Optional.ofNullable(this.getAetitlePairs().get(calledAET)).map(x -> x.containsKey(callingAET)).orElse(false);
    }

    /**
     * This method returns a {@link java.nio.file.Path} for the filesystem
     * workspace used by this application.
     * @return the workspace path
     */
    public Path getStorageDirPath() {
        return Paths.get(getStorageDir());
    }

    /**
     * This method returns a {@link java.nio.file.Path} for the filesystem
     * workspace directory used for storing temp files during incoming
     * transfers.
     * @return the workspace temp file path for incoming transfers
     */
    public Path getIncomingDirPath() {
        return this.getStorageDirPath().resolve(INCOMING_DIR);
    }

    /**
     * This method returns a {@link java.nio.file.Path} for the filesystem
     * workspace directory used for creating study zip archives and metadata
     * files.
     * @return the workspace for creating study zip archives and metadata files
     */
    public Path getZipDirPath() {
        return this.getStorageDirPath().resolve(ZIP_DIR);
    }

    /**
     * This method checks whether the given {@link java.nio.file.Path}
     * represents a filesystem workspace directory used for gathering dicom
     * files for each incoming study. The incoming study files are gathered in
     * directories named like:
     * &lt;storageDir&gt;/&lt;called AE title&gt;/&lt;calling AE title&gt;/&lt;study UID&gt;
     *
     * This method is not yet properly generalized, and was simply moved here
     * for reuse between the {@link StudyCompletionCheck} and the
     * {@link IncomingEndpoint}.
     * @param path expects a child of the storageDir filesystem workspace.
     * @return whether the given child path of the filesystem workspace is a directory for gathering incoming studies
     */
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
