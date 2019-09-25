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
        LOG.info("check for completed studies");
        try (Stream<Path> stream = Files.walk(this.storageDir, 1)) {
            stream.filter(this::workDir).forEach(System.out::println); //temp testing output
            stream.filter(this::workDir).forEach(this::checkAE);
        } catch(IOException e) {
            LOG.error("unable to open storage directory: " + this.storageDir, e);
        }
    }

    private void checkAE(Path aePath) {
        try (Stream<Path> stream = Files.list(aePath)) {
            stream.filter(Files::isDirectory).filter(this::isStudyComplete).forEach(this::processCompleteStudy);
        } catch(IOException e) {
            LOG.error("unable to check ae directory: " + aePath, e);
        }
    }

    private void processCompleteStudy(Path studyPath) {
        LOG.info("found complete study: {}", studyPath);
        Path study = studyPath.getFileName(); //value of study uid
        Path dest = this.zipDir.resolve(study);
        if (Files.exists(dest)) {
            throw new IllegalStateException("unable to move study dir '" + studyPath
              + "' for processing, destination '"+ dest + "' already exists");
        }
        try {
            Files.move(studyPath, dest);
        } catch (IOException e) {
            LOG.error("unable to move study dir for processing: " + studyPath, e);
        }
        processor.process(dest);
        LOG.info("finished processing complete study: " + study);
    }

    public boolean workDir(Path path) {
        if (!Files.isDirectory(path))
            return false;
        try {
            if (Files.isSameFile(this.storageDir, path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path to storage directory", e);
            return false;
        }
        try {
            if (Files.isSameFile(this.incomingDir, path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path to incoming directory", e);
            return false;
        }
        try {
            if (Files.isSameFile(this.zipDir, path))
                return false;
        } catch (IOException e) {
            LOG.error("error comparing path to zip directory", e);
            return false;
        }
        return true;
    }

    private boolean isStudyComplete(Path studyPath) {
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
        return studyLastModified > 0 && (System.currentTimeMillis() - studyLastModified > config.getStudyWaitTime());
    }
}
