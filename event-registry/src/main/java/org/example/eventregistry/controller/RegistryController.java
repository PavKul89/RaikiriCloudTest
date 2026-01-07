package org.example.eventregistry.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventregistry.entity.RegisteredEvent;
import org.example.eventregistry.service.EventProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
@Slf4j
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
    public ResponseEntity<Map<String, Object>> getEventsWithFilters(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(name = "direction", defaultValue = "DESC") String direction,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "serviceName", required = false) String serviceName) {

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<RegisteredEvent> eventsPage = eventService.getEventsWithFilters(
                pageable, startDate, endDate, eventType, serviceName
        );

        Map<String, Object> response = new HashMap<>();
        response.put("content", eventsPage.getContent());
        response.put("currentPage", eventsPage.getNumber());
        response.put("totalItems", eventsPage.getTotalElements());
        response.put("totalPages", eventsPage.getTotalPages());
        response.put("pageSize", eventsPage.getSize());
        response.put("hasNext", eventsPage.hasNext());
        response.put("hasPrevious", eventsPage.hasPrevious());

        Map<String, Object> filters = new HashMap<>();
        filters.put("startDate", startDate);
        filters.put("endDate", endDate);
        filters.put("eventType", eventType);
        filters.put("serviceName", serviceName);
        filters.put("sort", sort);
        filters.put("direction", direction);
        response.put("filters", filters);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/all")
    public ResponseEntity<?> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<RegisteredEvent> getEventById(@PathVariable(name = "id") String id) {
        try {
            UUID uuid = UUID.fromString(id);
            RegisteredEvent event = eventService.getEventById(uuid);
            if (event != null) {
                return ResponseEntity.ok(event);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", id);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/original/{originalId}")
    public ResponseEntity<RegisteredEvent> getEventByOriginalId(
            @PathVariable(name = "originalId") String originalId) {
        try {
            UUID uuid = UUID.fromString(originalId);
            RegisteredEvent event = eventService.getEventByOriginalId(uuid);
            if (event != null) {
                return ResponseEntity.ok(event);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", originalId);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/events/types")
    public ResponseEntity<?> getEventTypes() {
        return ResponseEntity.ok(eventService.getDistinctEventTypes());
    }

    @GetMapping("/events/services")
    public ResponseEntity<?> getServiceNames() {
        return ResponseEntity.ok(eventService.getDistinctServiceNames());
    }

    @GetMapping("/events/search")
    public ResponseEntity<?> searchEvents(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "originalId", required = false) String originalId) {

        if (id == null && originalId == null) {
            return ResponseEntity.badRequest().body("Please provide 'id' or 'originalId' parameter");
        }

        if (id != null) {
            try {
                UUID uuid = UUID.fromString(id);
                RegisteredEvent event = eventService.getEventById(uuid);
                if (event != null) {
                    return ResponseEntity.ok(event);
                }
                List<RegisteredEvent> events = searchEventsByPartialId(id);
                if (!events.isEmpty()) {
                    return ResponseEntity.ok(events);
                }
            } catch (IllegalArgumentException e) {
                List<RegisteredEvent> events = searchEventsByPartialId(id);
                if (!events.isEmpty()) {
                    return ResponseEntity.ok(events);
                }
            }
            return ResponseEntity.notFound().build();
        }

        if (originalId != null) {
            try {
                UUID uuid = UUID.fromString(originalId);
                RegisteredEvent event = eventService.getEventByOriginalId(uuid);
                if (event != null) {
                    return ResponseEntity.ok(event);
                }
                List<RegisteredEvent> events = searchEventsByPartialOriginalId(originalId);
                if (!events.isEmpty()) {
                    return ResponseEntity.ok(events);
                }
            } catch (IllegalArgumentException e) {
                List<RegisteredEvent> events = searchEventsByPartialOriginalId(originalId);
                if (!events.isEmpty()) {
                    return ResponseEntity.ok(events);
                }
            }
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.notFound().build();
    }

    private List<RegisteredEvent> searchEventsByPartialId(String partialId) {
        List<RegisteredEvent> allEvents = eventService.getAllEvents();
        return allEvents.stream()
                .filter(event -> event.getId().toString().contains(partialId))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<RegisteredEvent> searchEventsByPartialOriginalId(String partialOriginalId) {
        List<RegisteredEvent> allEvents = eventService.getAllEvents();
        return allEvents.stream()
                .filter(event -> event.getOriginalEventId().toString().contains(partialOriginalId))
                .collect(java.util.stream.Collectors.toList());
    }
}