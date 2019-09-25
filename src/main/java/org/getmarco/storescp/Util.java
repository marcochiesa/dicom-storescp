package org.getmarco.storescp;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.util.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {
    public static void logAssociation(Logger logger, Association association) {
        logger.info("state: {}", association.getState());
        String localAET = association.getLocalAET();
        String remoteAET = association.getRemoteAET();
        String remoteIP = association.getSocket().getInetAddress().getHostAddress();
        String remoteHost = association.getSocket().getInetAddress().getHostName();
        String remoteImplUID = association.getRemoteImplClassUID();
        String remoteImplVersion = association.getRemoteImplVersionName();
        logger.info("from ae: {}, ip: {}, host: {}", remoteAET, remoteIP, remoteHost);
        logger.info("implementation class uid: {}, version: {}", remoteImplUID, remoteImplVersion);
        logger.info("to ae: {}", localAET);
    }

    public static void logDicomFileAttributes(Logger logger, Attributes attributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("file[")
          .append("patientID=").append(attributes.getString(Tag.PatientID))
          .append(", patientName=").append(attributes.getString(Tag.PatientName))
          .append(", patientDob=").append(attributes.getString(Tag.PatientBirthDate))
          .append(", accessionNumber=").append(attributes.getString(Tag.AccessionNumber))
          .append(StringUtils.LINE_SEPARATOR)
          .append("studyUid=").append(attributes.getString(Tag.StudyInstanceUID))
          .append(StringUtils.LINE_SEPARATOR)
          .append("studyDesc=").append(attributes.getString(Tag.StudyDescription))
          .append(StringUtils.LINE_SEPARATOR)
          .append("modality=").append(attributes.getString(Tag.Modality))
          .append("]");
        logger.info(sb.toString());
    }

    public static Attributes parse(File file) throws IOException {
        try (DicomInputStream in = new DicomInputStream(file)) {
            in.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            return in.readDataset(-1, Tag.PixelData);
        }
    }

    public static void zipDir(Path sourceDir, Path zipFile) throws IOException {
        Files.createFile(zipFile);
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFile)); Stream<Path> stream = Files.walk(sourceDir)) {
            stream.filter(path -> !Files.isDirectory(path))
              .forEach(path -> {
                  ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                  try {
                      zs.putNextEntry(zipEntry);
                      Files.copy(path, zs);
                      zs.closeEntry();
                  } catch (IOException e) {
                      throw new RuntimeException("error writing file to zip: " + zipEntry, e);
                  }
              });
        }
    }
}
