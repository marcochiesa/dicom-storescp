package org.getmarco.storescp;

import org.dcm4che3.net.ApplicationEntity;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.TransferCapability;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class Starter implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Starter.class);

    @Autowired
    private Config config;
    @Autowired
    private Finisher finisher;
    @Autowired
    private CStoreSCP storeSCP;

    private Device device;
    private ApplicationEntity ae;
    private Connection conn;

    @Override
    public void run(String... args) throws Exception {
        String deviceName = config.getDeviceName();
        if (!StringUtils.hasText(deviceName))
            throw new IllegalArgumentException("empty device name");
        int port = config.getPort();
        if (port <= 0)
            throw new IllegalArgumentException("invalid port: " + port);
        LOG.info("creating device {} on port {}", deviceName, port);

        device = new Device(deviceName);
        ae = new ApplicationEntity("*");
        conn = new Connection();
        conn.setPort(port);
        conn.setTcpNoDelay(true);
        conn.setReceivePDULength(Connection.DEF_MAX_PDU_LENGTH);
        conn.setSendPDULength(Connection.DEF_MAX_PDU_LENGTH);
        conn.setMaxOpsInvoked(0);
        conn.setMaxOpsPerformed(0);
        device.addApplicationEntity(ae);
        device.addConnection(conn);
        ae.addConnection(conn);
        ae.setAssociationAcceptor(true);
        ae.addTransferCapability(
          new TransferCapability(null,
            "*",
            TransferCapability.Role.SCP,
            "*"));
        device.setDimseRQHandler(createServiceRegistry());
        ExecutorService executorService = Executors.newCachedThreadPool();
        ScheduledExecutorService scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor();
        device.setScheduledExecutor(scheduledExecutorService);
        device.setExecutor(executorService);
        device.bindConnections();
        LOG.info("device started");

        String storage = config.getStorageDir();
        LOG.info("checking storage directory: {}", storage);
        if (storage == null)
            fail("null storage directory");
        Path storageDir = Paths.get(storage);
        if (config.isClearStorageDirectoryOnStart()) {
            try {
                FileSystemUtils.deleteRecursively(storageDir);
            } catch (IOException ex) {
                LOG.error("unable to clear storage directory: " + storageDir, ex);
                finisher.finish(1);
            }
        }
        Files.createDirectories(storageDir);
        if (!Files.exists(storageDir))
            fail("unable to create storage directory: " + storageDir);

        Path incomingDir = storageDir.resolve(Config.INCOMING_DIR);
        Files.createDirectory(incomingDir);
        if (!Files.exists(incomingDir))
            fail("unable to create incoming directory: " + incomingDir);

        Path zipDir = storageDir.resolve(Config.ZIP_DIR);
        Files.createDirectory(zipDir);
        if (!Files.exists(zipDir))
            fail("unable to create incoming directory: " + zipDir);
    }

    private DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        serviceRegistry.addDicomService(storeSCP);
        return serviceRegistry;
    }

    private void fail(String message) {
        LOG.error(message);
        finisher.finish(1);
    }
}
