package org.getmarco.storescp;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.PDVInputStream;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCStoreSCP;
import org.dcm4che3.net.service.DicomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class CStoreSCP extends BasicCStoreSCP {
    private static final Logger LOG = LoggerFactory.getLogger(CStoreSCP.class);

    @Autowired
    private Config config;

    @Override
    protected void store(Association as, PresentationContext pc, Attributes rq, PDVInputStream data, Attributes rsp)
      throws IOException {
        LOG.info("receiving data to store");
        Util.logAssociation(LOG, as);

        // Check that local application is configured to accept files at the called ae
        // Also, check that the remote ae is allowed to transfer files to that ae
        String callingAET = as.getCallingAET();
        String calledAET = as.getCalledAET();
        if (!config.isAcceptedLocalAetitle(calledAET))
            throw new DicomServiceException(Status.NotAuthorized, "called ae title is unknown: " + calledAET);
        if (!config.hasAetitlePair(callingAET, calledAET))
            throw new DicomServiceException(Status.NotAuthorized, "calling ae title is unknown: " + callingAET);

        rsp.setInt(Tag.Status, VR.US, Status.Success);
        String storageDir = config.getStorageDir();
        String cuid = rq.getString(Tag.AffectedSOPClassUID);
        String iuid = rq.getString(Tag.AffectedSOPInstanceUID);
        String tsuid = pc.getTransferSyntax();
        // Store incoming file with path like <storage dir>/incoming/7c10c8cd-1536-4510-8cb7-c17570abe3dd.part
        Path incomingFile = Paths.get(storageDir, Config.INCOMING_DIR, UUID.randomUUID().toString() + Config.PART_EXT);
        try {
            storeTo(as, as.createFileMetaInformation(iuid, cuid, tsuid), data, incomingFile.toFile());
        } catch (Exception e) {
            deleteFile(as, incomingFile.toFile());
        }

        Attributes attributes = Util.parse(incomingFile);
        Util.logDicomFileAttributes(LOG, attributes);
        // Move to path like <storage dir>/SCP/SCU/1.2.840.xxxxx.3.152.235.2.12.187636473/1.2.840.xxxxx.3.152.235.2.12.187636473.dcm
        Path studyFile = Paths.get(storageDir, calledAET, callingAET, attributes.getString(Tag.StudyInstanceUID), iuid + Config.DCM_EXT);
        // Sanity check
        if (Files.exists(studyFile))
            throw new IllegalStateException("file already exists: " + studyFile);
        try {
            renameTo(as, incomingFile.toFile() , studyFile.toFile());
        } catch (Exception e) {
            deleteFile(as, incomingFile.toFile());
            deleteFile(as, studyFile.toFile());
        }

        int numCstoreRqReceived = as.getNumberOfReceived(Dimse.C_STORE_RQ);
        LOG.info("received {} cstore requests", numCstoreRqReceived);
    }

    private void storeTo(Association as, Attributes fmi, PDVInputStream data, File file) throws IOException  {
        LOG.info("{}: M-WRITE {}", as, file);
        file.getParentFile().mkdirs();
        try (DicomOutputStream out = new DicomOutputStream(file)) {
            out.writeFileMetaInformation(fmi);
            data.copyTo(out);
        }
    }

    private void renameTo(Association as, File from, File dest) throws IOException {
        LOG.info("{}: M-RENAME {} to {}", as, from, dest);
        if (!dest.getParentFile().mkdirs())
            dest.delete();
        if (!from.renameTo(dest))
            throw new IOException("Failed to rename " + from + " to " + dest);
    }

    private void deleteFile(Association as, File file) {
        if (file.delete())
            LOG.info("{}: M-DELETE {}", as, file);
        else
            LOG.warn("{}: M-DELETE {} failed!", as, file);
    }
}
