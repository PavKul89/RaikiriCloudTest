package org.example.eventgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.eventgenerator.entity.Event;
import org.example.eventgenerator.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Event testEvent;
    private UUID testEventId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testEventId = UUID.randomUUID();
        testEvent = new Event();
        testEvent.setId(testEventId);
        testEvent.setEventType("SYSTEM_EVENT");
        testEvent.setServiceName("event-generator");
        testEvent.setPayload("Test payload");
        testEvent.setCreatedAt(LocalDateTime.now());
        testEvent.setIsProcessed(false);
    }

    @Test
    void getStats_ShouldReturnStatistics() throws Exception {
        // Arrange
        when(eventService.getTotalEvents()).thenReturn(10L);
        when(eventService.getProcessedEventsCount()).thenReturn(7L);
        when(eventService.getUnprocessedEventsCount()).thenReturn(3L);

        mockMvc.perform(get("/api/events/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("event-generator"))
                .andExpect(jsonPath("$.totalEvents").value(10))
                .andExpect(jsonPath("$.processedEvents").value(7))
                .andExpect(jsonPath("$.unprocessedEvents").value(3))
                .andExpect(jsonPath("$.generationStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(eventService).getTotalEvents();
        verify(eventService).getProcessedEventsCount();
        verify(eventService).getUnprocessedEventsCount();
    }

    @Test
    void getAllEvents_ShouldReturnListOfEvents() throws Exception {
        // Arrange
        List<Event> events = Arrays.asList(testEvent);
        when(eventService.getAllEvents()).thenReturn(events);

        // Act & Assert
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()))
                .andExpect(jsonPath("$[0].eventType").value("SYSTEM_EVENT"))
                .andExpect(jsonPath("$[0].serviceName").value("event-generator"));

        verify(eventService).getAllEvents();
    }

    @Test
    void getEventById_WithValidUUID_ShouldReturnEvent() throws Exception {
        when(eventService.getEventById(testEventId)).thenReturn(testEvent);

        mockMvc.perform(get("/api/events/{id}", testEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()))
                .andExpect(jsonPath("$.eventType").value("SYSTEM_EVENT"));

        verify(eventService).getEventById(testEventId);
    }

    @Test
    void getEventById_WithInvalidUUID_ShouldReturnBadRequest() throws Exception {

        String invalidUuid = "invalid-uuid";

        mockMvc.perform(get("/api/events/{id}", invalidUuid))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventById(any());
    }

    @Test
    void getEventById_WithNonExistentUUID_ShouldReturnNotFound() throws Exception {

        when(eventService.getEventById(testEventId)).thenReturn(null);

        mockMvc.perform(get("/api/events/{id}", testEventId))
                .andExpect(status().isNotFound());

        verify(eventService).getEventById(testEventId);
    }

    @Test
    void generateEventManually_ShouldReturnCreatedEvent() throws Exception {

        when(eventService.generateEventManually(any(), any())).thenReturn(testEvent);

        mockMvc.perform(post("/api/events/generate")
                        .param("eventType", "MANUAL_EVENT")
                        .param("payload", "Custom payload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()))
                .andExpect(jsonPath("$.eventType").value("SYSTEM_EVENT"));

        verify(eventService).generateEventManually(eq("MANUAL_EVENT"), eq("Custom payload"));
    }

    @Test
    void generateEventManually_WithoutParameters_ShouldUseDefaults() throws Exception {

        when(eventService.generateEventManually(null, null)).thenReturn(testEvent);

        mockMvc.perform(post("/api/events/generate"))
                .andExpect(status().isOk());

        verify(eventService).generateEventManually(null, null);
    }

    @Test
    void generateEventManually_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {

        when(eventService.generateEventManually(any(), any())).thenThrow(new RuntimeException("DB error"));
        mockMvc.perform(post("/api/events/generate"))
                .andExpect(status().isInternalServerError());

        verify(eventService).generateEventManually(any(), any());
    }

    @Test
    void getProcessedEventsList_ShouldReturnProcessedEvents() throws Exception {

        testEvent.setIsProcessed(true);
        List<Event> processedEvents = Arrays.asList(testEvent);
        when(eventService.getProcessedEventsList()).thenReturn(processedEvents);

        mockMvc.perform(get("/api/events/processed/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()))
                .andExpect(jsonPath("$[0].isProcessed").value(true));

        verify(eventService).getProcessedEventsList();
    }

    @Test
    void getUnprocessedEventsList_ShouldReturnUnprocessedEvents() throws Exception {
        List<Event> unprocessedEvents = Arrays.asList(testEvent);
        when(eventService.getUnprocessedEventsList()).thenReturn(unprocessedEvents);

        mockMvc.perform(get("/api/events/unprocessed/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()))
                .andExpect(jsonPath("$[0].isProcessed").value(false));

        verify(eventService).getUnprocessedEventsList();
    }

    @Test
    void searchEvents_WithValidUUID_ShouldReturnEvent() throws Exception {

        when(eventService.getEventById(testEventId)).thenReturn(testEvent);

        mockMvc.perform(get("/api/events/search")
                        .param("id", testEventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()));

        verify(eventService).getEventById(testEventId);
        verify(eventService, never()).searchEventsByPartialId(any());
    }

    @Test
    void searchEvents_WithPartialId_ShouldReturnList() throws Exception {

        String partialId = "ff86";
        List<Event> events = Arrays.asList(testEvent);
        when(eventService.searchEventsByPartialId(partialId)).thenReturn(events);

        mockMvc.perform(get("/api/events/search")
                        .param("id", partialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()));

        verify(eventService, never()).getEventById(any());
        verify(eventService).searchEventsByPartialId(partialId);
    }

    @Test
    void searchEvents_WithoutId_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(get("/api/events/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Parameter 'id' is required"));

        verify(eventService, never()).getEventById(any());
        verify(eventService, never()).searchEventsByPartialId(any());
    }

    @Test
    void searchEvents_WithEmptyId_ShouldReturnBadRequest() throws Exception {

        mockMvc.perform(get("/api/events/search")
                        .param("id", ""))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventById(any());
        verify(eventService, never()).searchEventsByPartialId(any());
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/events/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("event-generator"))
                .andExpect(jsonPath("$.kafka").value("sending to: events.created"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
