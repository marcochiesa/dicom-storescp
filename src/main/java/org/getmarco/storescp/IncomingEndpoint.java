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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

    @Autowired
    private Config config;

    @ReadOperation
    public Map<String, ? extends Object> incoming() {
        Map<String, List<Map<String, String>>> map = new TreeMap<>();
        Path storageDir = config.getStorageDirPath();
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(config::isCalledAETDir).forEach( calledAETDir -> {
                String calledAET = calledAETDir.getFileName().toString();
                List<Map<String, String>> studies = checkCalledAETDir(calledAETDir);
                if (!studies.isEmpty())
                    map.put(calledAET, studies);
            });
        } catch(IOException e) {
            LOG.error("unable to process storage directory: " + storageDir, e);
            return Collections.singletonMap(ERROR_KEY, e.getMessage());
        }
        return map;
    }

    private List<Map<String, String>> checkCalledAETDir(Path calledAETDir) {
        List<Map<String, String>> list = new ArrayList<>();
        try (Stream<Path> callingAETDirs = Files.list(calledAETDir)) {
            callingAETDirs.forEach(callingAETDir -> {list.addAll(checkCallingAETDir(callingAETDir));});
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private List<Map<String, String>> checkCallingAETDir(Path callingAETDir) {
        List<Map<String, String>> list = new ArrayList<>();
        try (Stream<Path> studyDirs = Files.list(callingAETDir)) {
            studyDirs.forEach(studyDir -> {list.add(checkStudyDir(studyDir));});
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Map<String, String> checkStudyDir(Path studyDir) {
        Map<String, String> map = new HashMap<>();
        String studyUid = studyDir.getFileName().toString();
        long fileCount = countStudyFiles(studyDir);
        if (fileCount > 0) {
            map.put(STUDY_UID_KEY, studyUid);
            map.put(FILE_COUNT_KEY, String.valueOf(fileCount));
        }
        return map;
    }

    private long countStudyFiles(Path studyDir) {
        try (Stream<Path> studyFiles = Files.list(studyDir)) {
            return studyFiles.count();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
