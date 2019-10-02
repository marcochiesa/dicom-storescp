package org.getmarco.storescp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Convenience class to cleanly shutdown the Spring {@link org.springframework.context.ApplicationContext ApplicationContext}
 * and exit the JVM.
 */
@Component
public class Finisher {
    private static final Logger LOG = LoggerFactory.getLogger(Finisher.class);

    @Autowired
    private ApplicationContext applicationContext;

    public void finish() {
        finish(0);
    }

    public void finish(int returnCode) {
        LOG.info("shutting down application");
        int n = SpringApplication.exit(applicationContext, () -> returnCode);
        System.exit(n);
    }
}
