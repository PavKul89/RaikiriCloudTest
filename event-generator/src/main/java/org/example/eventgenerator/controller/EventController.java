package org.example.eventgenerator.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.service.EventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Value("${event.generation.enabled:true}")
    private boolean generationEnabled;
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = eventService.getTotalEvents();
        long processed = eventService.getProcessedEvents();
        long unprocessed = eventService.getUnprocessedEvents();

        stats.put("serviceName", "event-generator");
        stats.put("totalEvents", total);
        stats.put("processedEvents", processed);
        stats.put("unprocessedEvents", unprocessed);

        double processedPercentage = total > 0 ? (double) processed / total * 100 : 0;
        stats.put("processedPercentage", String.format("%.2f%%", processedPercentage));

        double unprocessedPercentage = total > 0 ? (double) unprocessed / total * 100 : 0;
        stats.put("unprocessedPercentage", String.format("%.2f%%", unprocessedPercentage));

        stats.put("generationStatus", generationEnabled ? "ACTIVE" : "PAUSED");
        stats.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
        Event event = eventService.getEventById(id);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<Event> generateEventManually(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String payload) {
        try {
            Event event = eventService.generateEventManually(eventType, payload);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}