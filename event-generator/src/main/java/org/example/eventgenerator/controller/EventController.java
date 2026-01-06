package org.example.eventgenerator.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceName", "event-generator");
        stats.put("totalEvents", eventService.getTotalEvents());
        stats.put("processedEvents", eventService.getProcessedEventsCount());
        stats.put("unprocessedEvents", eventService.getUnprocessedEventsCount());
        stats.put("generationStatus", "ACTIVE");
        stats.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable(name = "id") String id) {
        try {
            UUID uuid = UUID.fromString(id);
            Event event = eventService.getEventById(uuid);
            if (event != null) {
                return ResponseEntity.ok(event);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Event> generateEventManually(
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "payload", required = false) String payload) {
        try {
            Event event = eventService.generateEventManually(eventType, payload);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/processed/list")
    public ResponseEntity<List<Event>> getProcessedEventsList() {
        return ResponseEntity.ok(eventService.getProcessedEventsList());
    }

    @GetMapping("/unprocessed/list")
    public ResponseEntity<List<Event>> getUnprocessedEventsList() {
        return ResponseEntity.ok(eventService.getUnprocessedEventsList());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchEvents(@RequestParam(name = "id", required = false) String partialId) {
        if (partialId == null || partialId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Parameter 'id' is required");
        }

        try {
            UUID uuid = UUID.fromString(partialId);
            Event event = eventService.getEventById(uuid);
            if (event != null) {
                return ResponseEntity.ok(event);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {

            List<Event> events = eventService.searchEventsByPartialId(partialId);
            if (!events.isEmpty()) {
                return ResponseEntity.ok(events);
            }
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "event-generator");
        response.put("timestamp", java.time.LocalDateTime.now());
        response.put("kafka", "sending to: events.created");
        return ResponseEntity.ok(response);
    }
}