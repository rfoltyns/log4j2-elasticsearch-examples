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

@SpringBootApplication
public class Application {

    public static void main(String... args) {

        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);

        Logger logger = LogManager.getLogger("elasticsearch");

        ThreadContext.put("myFavouriteVariable", UUID.randomUUID().toString());
        ThreadContext.put("myStructuredJSON", "{\"yes\":100,\"you\":\"can\"}");
        logger.info("Hello, World!");
        ThreadContext.remove("myFavouriteVariable");
        ThreadContext.remove("myStructuredJSON");

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

        applicationContext.close();
        LoggingSystem.get(logger.getClass().getClassLoader()).getShutdownHandler().run();
    }

}
