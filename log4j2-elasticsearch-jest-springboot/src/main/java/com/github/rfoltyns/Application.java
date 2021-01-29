package com.github.rfoltyns;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@SpringBootApplication
public class Application {

    public static void main(String... args) throws JsonProcessingException {

        ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);

        Logger logger = LogManager.getLogger("elasticsearch");

        ObjectMapper objectMapper = new ObjectMapper();

        DataToSend data = new DataToSend("Hello, World!");
        String json = objectMapper.writeValueAsString(data);

        logger.info(json);

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

        applicationContext.close();
        LoggingSystem.get(logger.getClass().getClassLoader()).getShutdownHandler().run();
    }

    private static class DataToSend {

        final long timestamp = System.currentTimeMillis();
        final String message;

        private DataToSend(String message) {
            this.message = message;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }

    }

}
