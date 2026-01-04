package org.example.eventregistry.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventregistry.entity.RegisteredEvent;
import org.example.eventregistry.service.EventProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final EventProcessingService eventService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "event-registry");
        response.put("timestamp", LocalDateTime.now());
        response.put("kafka", "listening to: events.created");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceName", "event-registry");
        stats.put("totalRegisteredEvents", eventService.getTotalRegisteredEvents());
        stats.put("timestamp", LocalDateTime.now());
        stats.put("status", "ACTIVE");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/events")
    public ResponseEntity<List<RegisteredEvent>> getAllEvents() {
        List<RegisteredEvent> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<RegisteredEvent> getEventById(@PathVariable UUID id) {
        RegisteredEvent event = eventService.getEventById(id);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/events/original/{originalId}")
    public ResponseEntity<RegisteredEvent> getEventByOriginalId(@PathVariable UUID originalId) {
        RegisteredEvent event = eventService.getEventByOriginalId(originalId);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}