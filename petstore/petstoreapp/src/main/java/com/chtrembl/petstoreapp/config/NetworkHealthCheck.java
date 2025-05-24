package com.chtrembl.petstoreapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class NetworkHealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(NetworkHealthCheck.class);

    @EventListener(ApplicationReadyEvent.class)
    public void performHealthCheck() {
        try {
            InetAddress.getByName("www.google.com");
            logger.info("External network connectivity: OK");
        } catch (UnknownHostException e) {
            logger.warn("External network connectivity: FAILED - {}", e.getMessage());
        }
    }
}