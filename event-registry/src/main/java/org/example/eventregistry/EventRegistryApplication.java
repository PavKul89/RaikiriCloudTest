package org.example.eventregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class EventRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventRegistryApplication.class, args);

        System.out.println("\n==========================================");
        System.out.println("🚀 EVENT REGISTRY SERVICE STARTED");
        System.out.println("==========================================");
        System.out.println("🌐 REST API: http://localhost:8082");
        System.out.println("📊 Health check: http://localhost:8082/api/registry/health");
        System.out.println("📈 Statistics: http://localhost:8082/api/registry/stats");
        System.out.println("📋 All events: http://localhost:8082/api/registry/events");
        System.out.println("==========================================");
        System.out.println("📡 Kafka Consumer:");
        System.out.println("   Listening to: events.created");
        System.out.println("   Group ID: event-registry-group");
        System.out.println("==========================================");
        System.out.println("📤 Kafka Producer:");
        System.out.println("   Sending to: events.processed");
        System.out.println("==========================================");
        System.out.println("💾 Database:");
        System.out.println("   PostgreSQL: localhost:5432/events_db");
        System.out.println("   Table: registered_events");
        System.out.println("==========================================");
    }
}
