package org.example.eventregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventregistry.dto.EventResponse;
import org.example.eventregistry.entity.RegisteredEvent;
import org.example.eventregistry.repository.RegisteredEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProcessingService {

    private final RegisteredEventRepository eventRepository;
    private final KafkaTemplate<String, EventResponse> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "events.created",
            groupId = "event-registry-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void processEvent(String eventJson) {
        try {
            log.info("Received event JSON: {}", eventJson);

            EventData eventData = objectMapper.readValue(eventJson, EventData.class);

            log.info("=== START PROCESSING EVENT ===");
            log.info("Event ID: {}", eventData.getEventId());

            RegisteredEvent existingEvent = eventRepository
                    .findByOriginalEventId(eventData.getEventId());

            if (existingEvent != null) {
                log.warn("Event already registered: {}", eventData.getEventId());
                return;
            }

            RegisteredEvent registeredEvent = new RegisteredEvent();
            registeredEvent.setOriginalEventId(eventData.getEventId());
            registeredEvent.setEventType(eventData.getEventType());
            registeredEvent.setServiceName(eventData.getServiceName());
            registeredEvent.setPayload(eventData.getPayload());
            registeredEvent.setCreatedAt(eventData.getCreatedAt());
            registeredEvent.setProcessedAt(LocalDateTime.now());

            RegisteredEvent savedEvent = eventRepository.save(registeredEvent);
            log.info("✅ Event saved to DB with ID: {}", savedEvent.getId());

            EventResponse response = new EventResponse();
            response.setOriginalEventId(eventData.getEventId());
            response.setRegisteredEventId(savedEvent.getId());
            response.setStatus("PROCESSED");
            response.setProcessedAt(savedEvent.getProcessedAt());
            response.setRegistryServiceName("event-registry");

            kafkaTemplate.send("events.processed", response);
            log.info("📤 Confirmation sent for event: {}", eventData.getEventId());

        } catch (Exception e) {
            log.error("❌ Error processing event. JSON: {}", eventJson, e);
        }
    }

    public static class EventData {
        private UUID eventId;
        private String eventType;
        private String serviceName;
        private String payload;
        private LocalDateTime createdAt;
        public UUID getEventId() { return eventId; }
        public void setEventId(UUID eventId) { this.eventId = eventId; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public long getTotalRegisteredEvents() {
        return eventRepository.count();
    }

    public List<RegisteredEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    public RegisteredEvent getEventById(UUID id) {
        return eventRepository.findById(id).orElse(null);
    }

    public RegisteredEvent getEventByOriginalId(UUID originalId) {
        return eventRepository.findByOriginalEventId(originalId);
    }
}