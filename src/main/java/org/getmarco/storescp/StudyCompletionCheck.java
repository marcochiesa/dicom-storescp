package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

@Component
public class StudyCompletionCheck {
    private static final Logger LOG = LoggerFactory.getLogger(StudyCompletionCheck.class);

    @Autowired
    private Config config;
    @Autowired
    private StudyProcessor processor;
    private Path storageDir;
    private Path incomingDir;
    private Path zipDir;

    @PostConstruct
    private void setup() {
        this.storageDir = Paths.get(config.getStorageDir());
        this.incomingDir = this.storageDir.resolve(Config.INCOMING_DIR);
        this.zipDir = this.storageDir.resolve(Config.ZIP_DIR);
    }

    // Todo: set to one minute interval after testing
    @Scheduled(fixedDelay = 5000)
    public void checkCompletion() {
        LOG.debug("check for completed studies");
        try (Stream<Path> stream = Files.list(this.storageDir)) {
            stream.filter(this::isCalledAETDir).forEach(this::checkCalledAETDir);
        } catch(IOException e) {
            LOG.error("unable to open storage directory: " + this.storageDir, e);
        }
    }

    private void checkCalledAETDir(Path calledAETDir) {
        String calledAET = calledAETDir.getFileName().toString();
        try (Stream<Path> callingAETDirs = Files.list(calledAETDir)) {
            callingAETDirs.forEach(callingAETDir -> {
                String callingAET = callingAETDir.getFileName().toString();
                int studyWaitTime = config.getStudyWaitTime(callingAET, calledAET);
                try (Stream<Path> studyDirs = Files.list(callingAETDir)) {
                    studyDirs.forEach(studyDir -> {checkStudyDir(studyDir, studyWaitTime);});
                } catch(IOException e) {
                    LOG.error("unable to check calling AET directory: " + callingAETDir, e);
                }
            });
        } catch(IOException e) {
            LOG.error("unable to check called AET directory: " + calledAETDir, e);
        }
    }

    private void checkStudyDir(Path studyDir, int waitTime) {
        if (isStudyComplete(studyDir, waitTime))
            processCompleteStudy(studyDir);
    }

    private void processCompleteStudy(Path studyPath) {
        LOG.info("found complete study: {}", studyPath);
        Path dest = this.zipDir.resolve(UUID.randomUUID().toString());
        try {
            Files.move(studyPath, dest);
        } catch (IOException e) {
            LOG.error("unable to move study dir for processing: " + studyPath, e);
        }
        processor.process(dest);
        LOG.info("finished processing complete study: " + studyPath);
    }

    public boolean isCalledAETDir(Path path) {
        if (!Files.isDirectory(path))
            return false;
        try {
            if (Files.isSameFile(this.incomingDir, path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path '" + path + "' to incoming directory", e);
            return false;
        }
        try {
            if (Files.isSameFile(this.zipDir, path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path '" + path + "' to zip directory", e);
            return false;
        }
        return true;
    }

    private boolean isStudyComplete(Path studyPath, int studyWaitTime) {
        // Check if directory was last modified more than 'study wait time' milliseconds ago.
        //
        // File contents are not modified after the file is moved into the study directory
        // (from the incoming directory) so there should be no need to check the last modified
        // time of each file in the study directory.
        long studyLastModified = 0;
        try {
            studyLastModified = Files.getLastModifiedTime(studyPath).toMillis();
        } catch (IOException e) {
            LOG.error("unable to read modification time for study directory" + studyPath);
            return false;
        }
        return studyLastModified > 0 && (System.currentTimeMillis() - studyLastModified > studyWaitTime);
    }
}
