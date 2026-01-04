package org.example.eventgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventgenerator.dto.EventResponseDTO;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.repository.EventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConfirmationService {

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "events.processed",
            groupId = "event-generator-confirmation-group",
            containerFactory = "confirmationKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleConfirmation(String confirmationJson) {
        try {
            log.info("📨 Received confirmation: {}", confirmationJson);

            EventResponseDTO response = objectMapper.readValue(confirmationJson, EventResponseDTO.class);

            log.info("=== START PROCESSING CONFIRMATION ===");
            log.info("Original Event ID: {}", response.getOriginalEventId());
            log.info("Registry Event ID: {}", response.getRegisteredEventId());
            log.info("Status: {}", response.getStatus());
            log.info("Processed at: {}", response.getProcessedAt());

            Optional<Event> eventOpt = eventRepository.findById(response.getOriginalEventId());

            if (eventOpt.isPresent()) {
                Event event = eventOpt.get();

                if (Boolean.TRUE.equals(event.getIsProcessed())) {
                    log.warn("⚠️ Event already marked as processed: {}", event.getId());
                    return;
                }

                event.setIsProcessed(true);
                event.setProcessedAt(response.getProcessedAt());
                eventRepository.save(event);

                log.info("✅ Event marked as processed: {}", event.getId());
                log.info("Processed at: {}", event.getProcessedAt());
                log.info("=== CONFIRMATION PROCESSED SUCCESSFULLY ===");

            } else {
                log.error("❌ Event not found for confirmation: {}", response.getOriginalEventId());
            }

        } catch (Exception e) {
            log.error("❌ Error processing confirmation: {}", confirmationJson, e);
        }
    }

    public long getProcessedEventsCount() {
        return eventRepository.countByIsProcessed(true);
    }

    public long getUnprocessedEventsCount() {
        return eventRepository.countByIsProcessed(false);
    }
}
