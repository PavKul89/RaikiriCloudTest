package org.example.eventgenerator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class EventGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventGeneratorApplication.class, args);
        log.info("🚀 Event Generator started successfully!");
        log.info("📊 Statistics available at: http://localhost:8081/api/events/stats");
        log.info("📝 Events list available at: http://localhost:8081/api/events");
        log.info("⏰ Event generation scheduled every 10 seconds");
    }
}
