package org.getmarco.storescp;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.util.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Util {
    private Util() {
        // It's a utility method class
        throw new AssertionError("No Util instances for you!");
    }

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

    public static Attributes parse(Path dicomFile) throws IOException {
        try (DicomInputStream in = new DicomInputStream(dicomFile.toFile())) {
            in.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            return in.readDataset(-1, Tag.PixelData);
        }
    }

    public static Attributes parseDir(Path dicomDir) throws IOException {
        Attributes attributes = null;
        try (Stream<Path> stream = Files.list(dicomDir)) {
            attributes = stream.map(path -> {
                try {
                    return parse(path);
                } catch (IOException e) {
                    return null;
                }
            }).filter(Objects::nonNull).findAny().orElse(null);
        }
        if (attributes != null)
            return attributes;
        else
            throw new RuntimeException("unable to read dicom attributes from any file in directory: " + dicomDir);
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
