package org.example.eventgenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventgenerator.dto.EventMessage;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.repository.EventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final KafkaTemplate<String, EventMessage> kafkaTemplate;

    private static final String EVENT_TOPIC = "events.created";

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${event.generation.enabled:true}")
    private boolean generationEnabled;

    @Value("${event.generation.interval:10000}")
    private long generationInterval;

    /**
     * Генерирует событие по таймеру
     * Выполняется каждые 10 секунд по умолчанию
     */
    @Scheduled(fixedRateString = "${event.generation.interval:10000}")
    @Transactional
    public void generateEvent() {
        if (!generationEnabled) {
            log.debug("Event generation is disabled");
            return;
        }

        try {
            log.info("🚀 Starting event generation...");

            // 1. Создаем объект событие в базе данных
            Event event = new Event();
            event.setEventType("SYSTEM_EVENT");
            event.setServiceName(serviceName);
            event.setPayload(String.format("Auto-generated event at %s", LocalDateTime.now()));
            event.setIsProcessed(false);

            Event savedEvent = eventRepository.save(event);
            log.info("✅ Event created in database. ID: {}, Type: {}, Service: {}",
                    savedEvent.getId(), savedEvent.getEventType(), savedEvent.getServiceName());

            // 2. Создаем сообщение для Kafka
            EventMessage message = new EventMessage(
                    savedEvent.getId(),
                    savedEvent.getEventType(),
                    savedEvent.getServiceName(),
                    savedEvent.getPayload(),
                    savedEvent.getCreatedAt()
            );

            log.info("📤 Preparing to send Kafka message. Topic: {}, Event ID: {}",
                    EVENT_TOPIC, savedEvent.getId());

            // 3. Отправляем сообщение в Kafka
            kafkaTemplate.send(EVENT_TOPIC, message)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("✅ Event message sent to Kafka successfully. " +
                                            "Topic: {}, Partition: {}, Offset: {}, " +
                                            "Creator Service: {}, Event ID: {}",
                                    result.getRecordMetadata().topic(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset(),
                                    serviceName,
                                    savedEvent.getId());
                        } else {
                            log.error("❌ Failed to send event message to Kafka. Event ID: {}",
                                    savedEvent.getId(), ex);
                        }
                    });

            log.info("🎉 Event generation completed successfully. Event ID: {}", savedEvent.getId());

        } catch (Exception e) {
            log.error("❌ Error generating event", e);
        }
    }

    /**
     * Ручная генерация события (для тестирования)
     */
    @Transactional
    public Event generateEventManually(String eventType, String customPayload) {
        try {
            Event event = new Event();
            event.setEventType(eventType != null ? eventType : "MANUAL_EVENT");
            event.setServiceName(serviceName);
            event.setPayload(customPayload != null ? customPayload :
                    String.format("Manually generated at %s", LocalDateTime.now()));
            event.setIsProcessed(false);

            Event savedEvent = eventRepository.save(event);

            EventMessage message = new EventMessage(
                    savedEvent.getId(),
                    savedEvent.getEventType(),
                    savedEvent.getServiceName(),
                    savedEvent.getPayload(),
                    savedEvent.getCreatedAt()
            );

            kafkaTemplate.send(EVENT_TOPIC, message);
            log.info("Manual event generated: {}", savedEvent.getId());

            return savedEvent;
        } catch (Exception e) {
            log.error("Error in manual event generation", e);
            throw e;
        }
    }

    // Методы для статистики
    public long getTotalEvents() {
        return eventRepository.count();
    }

    public long getProcessedEvents() {
        return eventRepository.countByIsProcessed(true);
    }

    public long getUnprocessedEvents() {
        return eventRepository.countByIsProcessed(false);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(UUID id) {
        return eventRepository.findById(id).orElse(null);
    }
}
