package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@Component
@Endpoint(id = "incoming")
public class IncomingEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(IncomingEndpoint.class);

    public static final String STUDY_UID_KEY = "studyUid";
    public static final String FILE_COUNT_KEY = "fileCount";
    public static final String ERROR_KEY = "error";
    public static final String CAUSE_KEY = "cause";

    @Autowired
    private Config config;

    @ReadOperation
    public Map<String, Map<String, String>> incoming() {
        Map<String, Map<String, String>> map = new TreeMap<>();
        Path storageDir = config.getStorageDirPath();
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(config::isCalledAETDir).forEach( calledAETDir -> {
                String calledAET = calledAETDir.getFileName().toString();
                Map<String, String> studies = checkCalledAETDir(calledAETDir);
                if (!studies.isEmpty())
                    map.put(calledAET, studies);
            });
        } catch(IOException e) {
            LOG.error("unable to process storage directory: " + storageDir, e);
            Map <String, String> cause = Collections.<String, String>singletonMap(CAUSE_KEY, e.getMessage());
            return Collections.<String, Map<String, String>>singletonMap(ERROR_KEY, cause);
        }
        return map;
    }

    private Map<String, String> checkCalledAETDir(Path calledAETDir) {
        Map<String, String> map = new HashMap<>();
        try (Stream<Path> callingAETDirs = Files.list(calledAETDir)) {
            callingAETDirs.forEach(callingAETDir -> {
                try (Stream<Path> studyDirs = Files.list(callingAETDir)) {
                    studyDirs.forEach(studyDir -> {
                        String studyUid = studyDir.getFileName().toString();
                        long fileCount = checkStudyDir(studyDir);
                        if (fileCount > 0) {
                            map.put(STUDY_UID_KEY, studyUid);
                            map.put(FILE_COUNT_KEY, String.valueOf(fileCount));
                        }
                    });
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    private long checkStudyDir(Path studyDir) {
        try (Stream<Path> studyFiles = Files.list(studyDir)) {
            return studyFiles.count();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
