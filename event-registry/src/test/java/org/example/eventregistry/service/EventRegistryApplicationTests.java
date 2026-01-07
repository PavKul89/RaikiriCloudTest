package org.example.eventregistry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.eventregistry.dto.EventResponse;
import org.example.eventregistry.entity.RegisteredEvent;
import org.example.eventregistry.repository.RegisteredEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
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
class EventProcessingServiceTest {

    @Mock
    private RegisteredEventRepository eventRepository;

    @Mock
    private KafkaTemplate<String, EventResponse> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventProcessingService eventProcessingService;

    @Captor
    private ArgumentCaptor<RegisteredEvent> eventCaptor;

    @Captor
    private ArgumentCaptor<EventResponse> responseCaptor;

    private RegisteredEvent testRegisteredEvent;
    private UUID testEventId;
    private UUID testRegisteredEventId;
    private LocalDateTime testCreatedAt;
    private Object testEventData;
    private Class<?> eventDataClass;

    @BeforeEach
    void setUp() throws Exception {
        testEventId = UUID.randomUUID();
        testRegisteredEventId = UUID.randomUUID();
        testCreatedAt = LocalDateTime.now();

        eventDataClass = Class.forName(
                "org.example.eventregistry.service.EventProcessingService$EventData");

        Constructor<?> constructor = eventDataClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        testEventData = constructor.newInstance();

        ReflectionTestUtils.setField(testEventData, "eventId", testEventId);
        ReflectionTestUtils.setField(testEventData, "eventType", "SYSTEM_EVENT");
        ReflectionTestUtils.setField(testEventData, "serviceName", "event-generator");
        ReflectionTestUtils.setField(testEventData, "payload", "Test payload");
        ReflectionTestUtils.setField(testEventData, "createdAt", testCreatedAt);

        testRegisteredEvent = new RegisteredEvent();
        testRegisteredEvent.setId(testRegisteredEventId);
        testRegisteredEvent.setOriginalEventId(testEventId);
        testRegisteredEvent.setEventType("SYSTEM_EVENT");
        testRegisteredEvent.setServiceName("event-generator");
        testRegisteredEvent.setPayload("Test payload");
        testRegisteredEvent.setCreatedAt(testCreatedAt);
        testRegisteredEvent.setProcessedAt(testCreatedAt.plusMinutes(1));
    }

    @Test
    void processEvent_WhenValidEventJson_ShouldProcessAndSendResponse() throws Exception {
        // Arrange
        String eventJson = "{\"eventId\":\"" + testEventId + "\",\"eventType\":\"SYSTEM_EVENT\"," +
                "\"serviceName\":\"event-generator\",\"payload\":\"Test payload\"," +
                "\"createdAt\":\"" + testCreatedAt + "\"}";

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> testEventData);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);
        when(eventRepository.save(any(RegisteredEvent.class))).thenReturn(testRegisteredEvent);

        eventProcessingService.processEvent(eventJson);

        verify(objectMapper).readValue(eq(eventJson), eq(eventDataClass));
        verify(eventRepository).findByOriginalEventId(testEventId);
        verify(eventRepository).save(eventCaptor.capture());
        verify(kafkaTemplate).send(eq("events.processed"), responseCaptor.capture());

        RegisteredEvent savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent);
        assertEquals(testEventId, savedEvent.getOriginalEventId());
        assertEquals("SYSTEM_EVENT", savedEvent.getEventType());
        assertEquals("event-generator", savedEvent.getServiceName());
        assertEquals("Test payload", savedEvent.getPayload());
        assertEquals(testCreatedAt, savedEvent.getCreatedAt());
        assertNotNull(savedEvent.getProcessedAt());

        EventResponse response = responseCaptor.getValue();
        assertEquals(testEventId, response.getOriginalEventId());
        assertEquals(testRegisteredEventId, response.getRegisteredEventId());
        assertEquals("PROCESSED", response.getStatus());
        assertNotNull(response.getProcessedAt());
        assertEquals("event-registry", response.getRegistryServiceName());
    }

    @Test
    void processEvent_WhenEventAlreadyRegistered_ShouldSkipProcessing() throws Exception {

        String eventJson = "{\"eventId\":\"" + testEventId + "\"}";

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> testEventData);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(testRegisteredEvent);

        eventProcessingService.processEvent(eventJson);

        verify(objectMapper).readValue(eq(eventJson), eq(eventDataClass));
        verify(eventRepository).findByOriginalEventId(testEventId);
        verify(eventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void processEvent_WhenInvalidJson_ShouldLogError() throws Exception {

        String invalidJson = "invalid-json";

        when(objectMapper.readValue(eq(invalidJson), eq(eventDataClass)))
                .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        eventProcessingService.processEvent(invalidJson);

        verify(objectMapper).readValue(eq(invalidJson), eq(eventDataClass));
        verify(eventRepository, never()).findByOriginalEventId(any());
        verify(eventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void processEvent_WhenDatabaseError_ShouldLogError() throws Exception {

        String eventJson = "{\"eventId\":\"" + testEventId + "\"}";

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> testEventData);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);
        when(eventRepository.save(any(RegisteredEvent.class)))
                .thenThrow(new RuntimeException("Database error"));

        eventProcessingService.processEvent(eventJson);

        verify(objectMapper).readValue(eq(eventJson), eq(eventDataClass));
        verify(eventRepository).findByOriginalEventId(testEventId);
        verify(eventRepository).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    void processEvent_WhenKafkaSendError_ShouldLogError() throws Exception {

        String eventJson = "{\"eventId\":\"" + testEventId + "\"}";

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> testEventData);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);
        when(eventRepository.save(any(RegisteredEvent.class))).thenReturn(testRegisteredEvent);

        doThrow(new RuntimeException("Kafka error")).when(kafkaTemplate).send(eq("events.processed"), any());

        eventProcessingService.processEvent(eventJson);

        verify(objectMapper).readValue(eq(eventJson), eq(eventDataClass));
        verify(eventRepository).findByOriginalEventId(testEventId);
        verify(eventRepository).save(any());
        verify(kafkaTemplate).send(eq("events.processed"), any());
    }

    @Test
    void processEvent_WithNullPayload_ShouldProcessCorrectly() throws Exception {

        String eventJson = "{\"eventId\":\"" + testEventId + "\"}";

        Constructor<?> constructor = eventDataClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object eventDataWithNullPayload = constructor.newInstance();
        ReflectionTestUtils.setField(eventDataWithNullPayload, "eventId", testEventId);
        ReflectionTestUtils.setField(eventDataWithNullPayload, "eventType", "SYSTEM_EVENT");
        ReflectionTestUtils.setField(eventDataWithNullPayload, "serviceName", "event-generator");
        ReflectionTestUtils.setField(eventDataWithNullPayload, "payload", null);
        ReflectionTestUtils.setField(eventDataWithNullPayload, "createdAt", testCreatedAt);

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> eventDataWithNullPayload);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);
        when(eventRepository.save(any(RegisteredEvent.class))).thenReturn(testRegisteredEvent);

        eventProcessingService.processEvent(eventJson);

        verify(eventRepository).save(eventCaptor.capture());
        RegisteredEvent savedEvent = eventCaptor.getValue();
        assertNull(savedEvent.getPayload());
    }

    @Test
    void processEvent_WithNullCreatedAt_ShouldProcessCorrectly() throws Exception {

        String eventJson = "{\"eventId\":\"" + testEventId + "\"}";

        Constructor<?> constructor = eventDataClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object eventDataWithNullCreatedAt = constructor.newInstance();
        ReflectionTestUtils.setField(eventDataWithNullCreatedAt, "eventId", testEventId);
        ReflectionTestUtils.setField(eventDataWithNullCreatedAt, "eventType", "SYSTEM_EVENT");
        ReflectionTestUtils.setField(eventDataWithNullCreatedAt, "serviceName", "event-generator");
        ReflectionTestUtils.setField(eventDataWithNullCreatedAt, "payload", "Test payload");
        ReflectionTestUtils.setField(eventDataWithNullCreatedAt, "createdAt", null);

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenAnswer(invocation -> eventDataWithNullCreatedAt);

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);
        when(eventRepository.save(any(RegisteredEvent.class))).thenReturn(testRegisteredEvent);

        eventProcessingService.processEvent(eventJson);

        verify(eventRepository).save(eventCaptor.capture());
        RegisteredEvent savedEvent = eventCaptor.getValue();
        assertNull(savedEvent.getCreatedAt());
    }

    @Test
    void getEventsWithFilters_ShouldReturnFilteredPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        String eventType = "SYSTEM_EVENT";
        String serviceName = "event-generator";

        Page<RegisteredEvent> expectedPage = new PageImpl<>(Arrays.asList(testRegisteredEvent));
        when(eventRepository.findWithFilters(pageable, startDate, endDate, eventType, serviceName))
                .thenReturn(expectedPage);

        Page<RegisteredEvent> result = eventProcessingService.getEventsWithFilters(
                pageable, startDate, endDate, eventType, serviceName);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testRegisteredEvent, result.getContent().get(0));
        verify(eventRepository).findWithFilters(pageable, startDate, endDate, eventType, serviceName);
    }

    @Test
    void getEventsWithFilters_WithNullParameters_ShouldCallRepositoryWithNulls() {

        Pageable pageable = PageRequest.of(0, 10);
        Page<RegisteredEvent> expectedPage = new PageImpl<>(Arrays.asList(testRegisteredEvent));
        when(eventRepository.findWithFilters(pageable, null, null, null, null))
                .thenReturn(expectedPage);

        Page<RegisteredEvent> result = eventProcessingService.getEventsWithFilters(
                pageable, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).findWithFilters(pageable, null, null, null, null);
    }

    @Test
    void getTotalRegisteredEvents_ShouldReturnCount() {

        long expectedCount = 100L;
        when(eventRepository.count()).thenReturn(expectedCount);

        long result = eventProcessingService.getTotalRegisteredEvents();

        assertEquals(expectedCount, result);
        verify(eventRepository).count();
    }

    @Test
    void getAllEvents_ShouldReturnAllEvents() {

        List<RegisteredEvent> expectedEvents = Arrays.asList(testRegisteredEvent);
        when(eventRepository.findAll()).thenReturn(expectedEvents);

        List<RegisteredEvent> result = eventProcessingService.getAllEvents();

        assertEquals(1, result.size());
        assertEquals(testRegisteredEvent, result.get(0));
        verify(eventRepository).findAll();
    }

    @Test
    void getEventById_WithExistingId_ShouldReturnEvent() {

        when(eventRepository.findById(testRegisteredEventId))
                .thenReturn(Optional.of(testRegisteredEvent));

        RegisteredEvent result = eventProcessingService.getEventById(testRegisteredEventId);

        assertNotNull(result);
        assertEquals(testRegisteredEvent, result);
        verify(eventRepository).findById(testRegisteredEventId);
    }

    @Test
    void getEventById_WithNonExistingId_ShouldReturnNull() {
        // Arrange
        when(eventRepository.findById(testRegisteredEventId)).thenReturn(Optional.empty());

        RegisteredEvent result = eventProcessingService.getEventById(testRegisteredEventId);

        assertNull(result);
        verify(eventRepository).findById(testRegisteredEventId);
    }

    @Test
    void getEventByOriginalId_WithExistingOriginalId_ShouldReturnEvent() {

        when(eventRepository.findByOriginalEventId(testEventId))
                .thenReturn(testRegisteredEvent);

        RegisteredEvent result = eventProcessingService.getEventByOriginalId(testEventId);

        assertNotNull(result);
        assertEquals(testRegisteredEvent, result);
        verify(eventRepository).findByOriginalEventId(testEventId);
    }

    @Test
    void getEventByOriginalId_WithNonExistingOriginalId_ShouldReturnNull() {

        when(eventRepository.findByOriginalEventId(testEventId)).thenReturn(null);

        RegisteredEvent result = eventProcessingService.getEventByOriginalId(testEventId);

        assertNull(result);
        verify(eventRepository).findByOriginalEventId(testEventId);
    }

    @Test
    void getDistinctEventTypes_ShouldReturnListOfTypes() {

        List<String> expectedTypes = Arrays.asList("SYSTEM_EVENT", "USER_EVENT", "ERROR_EVENT");
        when(eventRepository.findDistinctEventTypes()).thenReturn(expectedTypes);

        List<String> result = eventProcessingService.getDistinctEventTypes();

        assertEquals(3, result.size());
        assertTrue(result.contains("SYSTEM_EVENT"));
        assertTrue(result.contains("USER_EVENT"));
        assertTrue(result.contains("ERROR_EVENT"));
        verify(eventRepository).findDistinctEventTypes();
    }

    @Test
    void getDistinctServiceNames_ShouldReturnListOfServices() {

        List<String> expectedServices = Arrays.asList("event-generator", "user-service", "auth-service");
        when(eventRepository.findDistinctServiceNames()).thenReturn(expectedServices);

        List<String> result = eventProcessingService.getDistinctServiceNames();

        assertEquals(3, result.size());
        assertTrue(result.contains("event-generator"));
        assertTrue(result.contains("user-service"));
        assertTrue(result.contains("auth-service"));
        verify(eventRepository).findDistinctServiceNames();
    }

    @Test
    void getDistinctEventTypes_WhenEmpty_ShouldReturnEmptyList() {

        List<String> expectedTypes = Arrays.asList();
        when(eventRepository.findDistinctEventTypes()).thenReturn(expectedTypes);

        List<String> result = eventProcessingService.getDistinctEventTypes();

        assertTrue(result.isEmpty());
        verify(eventRepository).findDistinctEventTypes();
    }

    @Test
    void getDistinctServiceNames_WhenEmpty_ShouldReturnEmptyList() {

        List<String> expectedServices = Arrays.asList();
        when(eventRepository.findDistinctServiceNames()).thenReturn(expectedServices);

        List<String> result = eventProcessingService.getDistinctServiceNames();

        assertTrue(result.isEmpty());
        verify(eventRepository).findDistinctServiceNames();
    }

    @Test
    void processEvent_ShouldHandleJsonProcessingException() throws Exception {

        String eventJson = "{\"invalid\":json}";

        when(objectMapper.readValue(eq(eventJson), eq(eventDataClass)))
                .thenThrow(new com.fasterxml.jackson.databind.JsonMappingException(null, "Invalid JSON"));

        eventProcessingService.processEvent(eventJson);

        verify(objectMapper).readValue(eq(eventJson), eq(eventDataClass));
        verify(eventRepository, never()).findByOriginalEventId(any());
        verify(eventRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }
}