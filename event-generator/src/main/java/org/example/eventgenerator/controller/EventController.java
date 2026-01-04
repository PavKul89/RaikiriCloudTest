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
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Статистика по событиям
     * GET /api/events/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceName", "event-generator");
        stats.put("totalEvents", eventService.getTotalEvents());
        stats.put("processedEvents", eventService.getProcessedEvents());
        stats.put("unprocessedEvents", eventService.getUnprocessedEvents());
        stats.put("generationStatus", "ACTIVE");
        stats.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(stats);
    }

    /**
     * Получить все события
     * GET /api/events
     */
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * Получить событие по ID
     * GET /api/events/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable UUID id) { //:TODO dto
        Event event = eventService.getEventById(id);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Ручная генерация события
     * POST /api/events/generate
     */
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
