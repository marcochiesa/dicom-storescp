package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * This class runs a scheduled check to determine if incoming studies are
 * 'complete'. If they are (no new files received within configured wait time),
 *  then move them to the workarea for processing and trigger processing.
 */
@Component
public class StudyCompletionCheck {
    private static final Logger LOG = LoggerFactory.getLogger(StudyCompletionCheck.class);

    @Autowired
    private Config config;
    @Autowired
    private StudyProcessor processor;

    /**
     * Check the filesystem workspace for 'complete' studies.
     */
    // Todo: set to one minute interval after testing
    @Scheduled(fixedDelay = 5000)
    public void checkCompletion() {
        LOG.debug("check for completed studies");
        Path storageDir = config.getStorageDirPath();
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(config::isCalledAETDir).forEach(this::checkCalledAETDir);
        } catch(IOException e) {
            LOG.error("unable to open storage directory: " + storageDir, e);
        }
    }

    // Check whether studies received for a particular AE Title are 'complete'.
    private void checkCalledAETDir(Path calledAETDir) {
        try (Stream<Path> callingAETDirs = Files.list(calledAETDir)) {
            callingAETDirs.forEach(this::checkCallingAETDir);
        } catch(IOException e) {
            LOG.error("unable to check called AET directory: " + calledAETDir, e);
        }
    }
    // Check whether studies received from a particular AE Title are 'complete'.
    private void checkCallingAETDir(Path callingAETDir) {
        String calledAET = callingAETDir.getParent().getFileName().toString();
        String callingAET = callingAETDir.getFileName().toString();
        int studyWaitTime = config.getStudyWaitTime(callingAET, calledAET);
        try (Stream<Path> studyDirs = Files.list(callingAETDir)) {
            studyDirs.forEach(studyDir -> {checkStudyDir(studyDir, studyWaitTime);});
        } catch(IOException e) {
            LOG.error("unable to check calling AET directory: " + callingAETDir, e);
        }
    }

    // Check if a given study is 'complete'.
    private void checkStudyDir(Path studyDir, int studyWaitTime) {
        if (isStudyComplete(studyDir, studyWaitTime))
            processCompleteStudy(studyDir);
    }

    // Prepare the 'completed' study for processing, and trigger processing.
    private void processCompleteStudy(Path studyPath) {
        LOG.info("found complete study: {}", studyPath);
        Path dest = config.getZipDirPath().resolve(UUID.randomUUID().toString());
        try {
            Files.move(studyPath, dest);
        } catch (IOException e) {
            LOG.error("unable to move study dir for processing: " + studyPath, e);
        }
        processor.process(dest);
        LOG.info("finished processing complete study: " + studyPath);
    }

    // Check if the given filesystem path (assumed to be a directory holding
    // dicom files for an incoming study) has not been modified within the
    // configured study wait time.
    private boolean isStudyComplete(Path studyDir, int studyWaitTime) {
        // Check if directory was last modified more than 'study wait time' milliseconds ago.
        //
        // File contents are not modified after the file is moved into the study directory
        // (from the incoming directory) so there should be no need to check the last modified
        // time of each file in the study directory.
        long studyLastModified = 0;
        try {
            studyLastModified = Files.getLastModifiedTime(studyDir).toMillis();
        } catch (IOException e) {
            LOG.error("unable to read modification time for study directory" + studyDir);
            return false;
        }
        return studyLastModified > 0 && (System.currentTimeMillis() - studyLastModified > studyWaitTime);
    }
}
