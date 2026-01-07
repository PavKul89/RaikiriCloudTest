package org.example.eventgenerator.service;

import org.example.eventgenerator.dto.EventMessage;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, EventMessage> kafkaTemplate;

    @InjectMocks
    private EventService eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Captor
    private ArgumentCaptor<EventMessage> messageCaptor;

    private Event testEvent;
    private UUID testEventId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventService, "serviceName", "event-generator");
        ReflectionTestUtils.setField(eventService, "generationEnabled", true);


        testEventId = UUID.randomUUID();
        testEvent = new Event();
        testEvent.setId(testEventId);
        testEvent.setEventType("SYSTEM_EVENT");
        testEvent.setServiceName("event-generator");
        testEvent.setPayload("Test payload");
        testEvent.setCreatedAt(LocalDateTime.now());
        testEvent.setIsProcessed(false);
        testEvent.setProcessedAt(null);
    }

    @Test
    void generateEvent_WhenGenerationEnabled_ShouldSaveEventAndSendToKafka() {

        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.generateEvent();

        verify(eventRepository).save(eventCaptor.capture());
        verify(kafkaTemplate).send(eq("events.created"), messageCaptor.capture());

        Event savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent);
        assertEquals("SYSTEM_EVENT", savedEvent.getEventType());
        assertEquals("event-generator", savedEvent.getServiceName());
        assertTrue(savedEvent.getPayload().contains("Auto-generated event at"));
        assertFalse(savedEvent.getIsProcessed());
        assertNull(savedEvent.getProcessedAt());

        EventMessage sentMessage = messageCaptor.getValue();
        assertEquals(testEventId, sentMessage.getEventId());
        assertEquals("SYSTEM_EVENT", sentMessage.getEventType());
        assertEquals("event-generator", sentMessage.getServiceName());
        assertEquals(testEvent.getPayload(), sentMessage.getPayload());
        assertEquals(testEvent.getCreatedAt(), sentMessage.getCreatedAt());
    }

    @Test
    void generateEvent_WhenGenerationDisabled_ShouldNotGenerate() {

        ReflectionTestUtils.setField(eventService, "generationEnabled", false);

        eventService.generateEvent();

        verify(eventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void generateEvent_WhenExceptionThrown_ShouldLogError() {

        when(eventRepository.save(any(Event.class))).thenThrow(new RuntimeException("DB error"));

        eventService.generateEvent();

        verify(eventRepository).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void generateEventManually_WithCustomParameters_ShouldSaveAndSendEvent() {

        String customEventType = "CUSTOM_EVENT";
        String customPayload = "Custom manual event";
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.generateEventManually(customEventType, customPayload);

        verify(eventRepository).save(eventCaptor.capture());
        verify(kafkaTemplate).send(eq("events.created"), messageCaptor.capture());

        Event savedEvent = eventCaptor.getValue();
        assertEquals(customEventType, savedEvent.getEventType());
        assertEquals("event-generator", savedEvent.getServiceName());
        assertEquals(customPayload, savedEvent.getPayload());
        assertFalse(savedEvent.getIsProcessed());

        assertEquals(testEvent, result);
    }

    @Test
    void generateEventManually_WithNullParameters_ShouldUseDefaults() {

        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.generateEventManually(null, null);

        verify(eventRepository).save(eventCaptor.capture());

        Event savedEvent = eventCaptor.getValue();
        assertEquals("MANUAL_EVENT", savedEvent.getEventType());
        assertEquals("event-generator", savedEvent.getServiceName());
        assertTrue(savedEvent.getPayload().contains("Manually generated at"));
        assertFalse(savedEvent.getIsProcessed());

        assertEquals(testEvent, result);
    }

    @Test
    void generateEventManually_WhenExceptionThrown_ShouldRethrow() {

        when(eventRepository.save(any(Event.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () ->
                eventService.generateEventManually("TEST", "payload"));

        verify(eventRepository).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void getTotalEvents_ShouldReturnCount() {

        long expectedCount = 42L;
        when(eventRepository.count()).thenReturn(expectedCount);

        long result = eventService.getTotalEvents();

        assertEquals(expectedCount, result);
        verify(eventRepository).count();
    }

    @Test
    void getProcessedEventsCount_ShouldReturnCountOfProcessedEvents() {

        long expectedCount = 25L;
        when(eventRepository.countByIsProcessed(true)).thenReturn(expectedCount);

        long result = eventService.getProcessedEventsCount();

        assertEquals(expectedCount, result);
        verify(eventRepository).countByIsProcessed(true);
    }

    @Test
    void getUnprocessedEventsCount_ShouldReturnCountOfUnprocessedEvents() {

        long expectedCount = 17L;
        when(eventRepository.countByIsProcessed(false)).thenReturn(expectedCount);

        long result = eventService.getUnprocessedEventsCount();

        assertEquals(expectedCount, result);
        verify(eventRepository).countByIsProcessed(false);
    }

    @Test
    void getProcessedEventsList_ShouldReturnListOfProcessedEvents() {

        testEvent.setIsProcessed(true);
        List<Event> processedEvents = Arrays.asList(testEvent);
        when(eventRepository.findByIsProcessedTrue()).thenReturn(processedEvents);

        List<Event> result = eventService.getProcessedEventsList();

        assertEquals(1, result.size());
        assertEquals(testEvent, result.get(0));
        assertTrue(result.get(0).getIsProcessed());
        verify(eventRepository).findByIsProcessedTrue();
    }

    @Test
    void getUnprocessedEventsList_ShouldReturnListOfUnprocessedEvents() {

        List<Event> unprocessedEvents = Arrays.asList(testEvent);
        when(eventRepository.findByIsProcessedFalse()).thenReturn(unprocessedEvents);

        List<Event> result = eventService.getUnprocessedEventsList();

        assertEquals(1, result.size());
        assertEquals(testEvent, result.get(0));
        assertFalse(result.get(0).getIsProcessed());
        verify(eventRepository).findByIsProcessedFalse();
    }

    @Test
    void getAllEvents_ShouldReturnAllEvents() {

        List<Event> allEvents = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.getAllEvents();

        assertEquals(1, result.size());
        assertEquals(testEvent, result.get(0));
        verify(eventRepository).findAll();
    }

    @Test
    void getEventById_WithExistingId_ShouldReturnEvent() {

        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));

        Event result = eventService.getEventById(testEventId);

        assertNotNull(result);
        assertEquals(testEvent, result);
        verify(eventRepository).findById(testEventId);
    }

    @Test
    void getEventById_WithNonExistingId_ShouldReturnNull() {

        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        Event result = eventService.getEventById(testEventId);

        assertNull(result);
        verify(eventRepository).findById(testEventId);
    }

    @Test
    void searchEventsByPartialId_WithMatchingPartialId_ShouldReturnEvents() {

        String partialId = testEventId.toString().substring(0, 8);
        List<Event> allEvents = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.searchEventsByPartialId(partialId);

        assertEquals(1, result.size());
        assertEquals(testEvent, result.get(0));
        verify(eventRepository).findAll();
    }

    @Test
    void searchEventsByPartialId_WithNonMatchingPartialId_ShouldReturnEmptyList() {
        String partialId = "NONEXISTENT";
        List<Event> allEvents = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.searchEventsByPartialId(partialId);

        assertTrue(result.isEmpty());
        verify(eventRepository).findAll();
    }

    @Test
    void searchEventsByPartialId_WithEmptyPartialId_ShouldReturnAllEvents() {

        String partialId = "";
        List<Event> allEvents = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.searchEventsByPartialId(partialId);

        assertEquals(1, result.size());
        assertEquals(testEvent, result.get(0));
        verify(eventRepository).findAll();
    }

    @Test
    void searchEventsByPartialId_WhenRepositoryReturnsEmptyList_ShouldReturnEmptyList() {

        String partialId = "anything";
        when(eventRepository.findAll()).thenReturn(Arrays.asList());

        List<Event> result = eventService.searchEventsByPartialId(partialId);

        assertTrue(result.isEmpty());
        verify(eventRepository).findAll();
    }
}