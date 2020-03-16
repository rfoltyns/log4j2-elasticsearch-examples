package com.github.rfoltyns;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@SpringBootApplication
public class Application {

    public static void main(String... args) {

        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);

        Logger logger = LogManager.getLogger("elasticsearch");
        logger.info("Hello, World!");

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

        applicationContext.close();
        LoggingSystem.get(logger.getClass().getClassLoader()).getShutdownHandler().run();
    }

}
