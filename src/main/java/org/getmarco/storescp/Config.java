package org.getmarco.storescp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storescp")
public class Config {
    public static final String PART_EXT = ".part";
    public static final String DCM_EXT = ".dcm";
    public static final String ZIP_EXT = ".zip";
    public static final String TXT_EXT = ".txt";
    public static final String INCOMING_DIR = "incoming";
    public static final String ZIP_DIR = "zip";

    private String deviceName;
    private String storageDir;
    private int port;
    private int studyWaitTime;
    private boolean clearStorageDirectoryOnStart;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getStudyWaitTime() {
        return studyWaitTime;
    }

    public void setStudyWaitTime(int studyWaitTime) {
        this.studyWaitTime = studyWaitTime;
    }

    public boolean isClearStorageDirectoryOnStart() {
        return clearStorageDirectoryOnStart;
    }

    public void setClearStorageDirectoryOnStart(boolean clearStorageDirectoryOnStart) {
        this.clearStorageDirectoryOnStart = clearStorageDirectoryOnStart;
    }
}
