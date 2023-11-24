package com.github.rfoltyns;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class NonStopApplication {

    public static void main(String... args) {
        LogManager.getLogger("elasticsearch").error("Hello world!");
        LogManager.shutdown();
    }

}
