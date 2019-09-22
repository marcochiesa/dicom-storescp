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
import org.springframework.util.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class StorescpStarter implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(StorescpStarter.class);

    @Autowired
    private Config config;
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
    }

    private DicomServiceRegistry createServiceRegistry() {
        DicomServiceRegistry serviceRegistry = new DicomServiceRegistry();
        serviceRegistry.addDicomService(new BasicCEchoSCP());
        return serviceRegistry;
    }
}
