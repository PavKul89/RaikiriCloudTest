package org.example.eventregistry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.eventregistry.entity.RegisteredEvent;
import org.example.eventregistry.service.EventProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistryControllerTest {

    @Mock
    private EventProcessingService eventService;

    @InjectMocks
    private RegistryController registryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private RegisteredEvent testRegisteredEvent;
    private UUID testEventId;
    private UUID testOriginalEventId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registryController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testEventId = UUID.randomUUID();
        testOriginalEventId = UUID.randomUUID();

        testRegisteredEvent = new RegisteredEvent();
        testRegisteredEvent.setId(testEventId);
        testRegisteredEvent.setOriginalEventId(testOriginalEventId);
        testRegisteredEvent.setEventType("SYSTEM_EVENT");
        testRegisteredEvent.setServiceName("event-generator");
        testRegisteredEvent.setPayload("Test payload");
        testRegisteredEvent.setCreatedAt(LocalDateTime.now());
        testRegisteredEvent.setRegisteredAt(LocalDateTime.now());
        testRegisteredEvent.setProcessedAt(LocalDateTime.now());
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/registry/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("event-registry"))
                .andExpect(jsonPath("$.kafka").value("listening to: events.created"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getStats_ShouldReturnStatistics() throws Exception {
        // Arrange
        when(eventService.getTotalRegisteredEvents()).thenReturn(15L);

        // Act & Assert
        mockMvc.perform(get("/api/registry/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("event-registry"))
                .andExpect(jsonPath("$.totalRegisteredEvents").value(15))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(eventService).getTotalRegisteredEvents();
    }

    @Test
    void getEventsWithFilters_WithDefaultParameters_ShouldReturnPaginatedEvents() throws Exception {
        // Arrange
        List<RegisteredEvent> events = Arrays.asList(testRegisteredEvent);
        Page<RegisteredEvent> page = new PageImpl<>(events, PageRequest.of(0, 20), 1);

        when(eventService.getEventsWithFilters(
                any(Pageable.class),
                eq(null), eq(null), eq(null), eq(null))
        ).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testEventId.toString()))
                .andExpect(jsonPath("$.content[0].originalEventId").value(testOriginalEventId.toString()))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false))
                .andExpect(jsonPath("$.filters.sort").value("createdAt"))
                .andExpect(jsonPath("$.filters.direction").value("DESC"));

        verify(eventService).getEventsWithFilters(
                any(Pageable.class),
                eq(null), eq(null), eq(null), eq(null));
    }

    @Test
    void getAllEvents_ShouldReturnAllEvents() throws Exception {
        // Arrange
        List<RegisteredEvent> events = Arrays.asList(testRegisteredEvent);
        when(eventService.getAllEvents()).thenReturn(events);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()))
                .andExpect(jsonPath("$[0].originalEventId").value(testOriginalEventId.toString()));

        verify(eventService).getAllEvents();
    }

    @Test
    void getEventById_WithValidUUID_ShouldReturnEvent() throws Exception {
        // Arrange
        when(eventService.getEventById(testEventId)).thenReturn(testRegisteredEvent);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/{id}", testEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()))
                .andExpect(jsonPath("$.originalEventId").value(testOriginalEventId.toString()));

        verify(eventService).getEventById(testEventId);
    }

    @Test
    void getEventById_WithInvalidUUID_ShouldReturnBadRequest() throws Exception {
        // Arrange
        String invalidUuid = "invalid-uuid";

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/{id}", invalidUuid))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).getEventById(any());
    }

    @Test
    void getEventById_WithNonExistentUUID_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(eventService.getEventById(testEventId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/{id}", testEventId))
                .andExpect(status().isNotFound());

        verify(eventService).getEventById(testEventId);
    }

    @Test
    void getEventByOriginalId_WithValidUUID_ShouldReturnEvent() throws Exception {
        // Arrange
        when(eventService.getEventByOriginalId(testOriginalEventId)).thenReturn(testRegisteredEvent);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/original/{originalId}", testOriginalEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()))
                .andExpect(jsonPath("$.originalEventId").value(testOriginalEventId.toString()));

        verify(eventService).getEventByOriginalId(testOriginalEventId);
    }

    @Test
    void getEventTypes_ShouldReturnDistinctEventTypes() throws Exception {
        // Arrange
        List<String> eventTypes = Arrays.asList("SYSTEM_EVENT", "MANUAL_EVENT", "ERROR_EVENT");
        when(eventService.getDistinctEventTypes()).thenReturn(eventTypes);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("SYSTEM_EVENT"))
                .andExpect(jsonPath("$[1]").value("MANUAL_EVENT"))
                .andExpect(jsonPath("$[2]").value("ERROR_EVENT"));

        verify(eventService).getDistinctEventTypes();
    }

    @Test
    void getServiceNames_ShouldReturnDistinctServiceNames() throws Exception {
        // Arrange
        List<String> serviceNames = Arrays.asList("event-generator", "user-service", "payment-service");
        when(eventService.getDistinctServiceNames()).thenReturn(serviceNames);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("event-generator"))
                .andExpect(jsonPath("$[1]").value("user-service"))
                .andExpect(jsonPath("$[2]").value("payment-service"));

        verify(eventService).getDistinctServiceNames();
    }

    @Test
    void searchEvents_WithIdParameter_ShouldSearchById() throws Exception {
        // Arrange
        when(eventService.getEventById(testEventId)).thenReturn(testRegisteredEvent);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search")
                        .param("id", testEventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()));

        verify(eventService).getEventById(testEventId);
    }

    @Test
    void searchEvents_WithOriginalIdParameter_ShouldSearchByOriginalId() throws Exception {
        // Arrange
        when(eventService.getEventByOriginalId(testOriginalEventId)).thenReturn(testRegisteredEvent);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search")
                        .param("originalId", testOriginalEventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()))
                .andExpect(jsonPath("$.originalEventId").value(testOriginalEventId.toString()));

        verify(eventService).getEventByOriginalId(testOriginalEventId);
    }

    @Test
    void searchEvents_WithInvalidUUID_ShouldReturnNotFound() throws Exception {
        // Arrange
        String invalidUuid = "invalid";
        when(eventService.getAllEvents()).thenReturn(Collections.singletonList(testRegisteredEvent));

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search")
                        .param("id", invalidUuid))
                .andExpect(status().isNotFound());

        verify(eventService).getAllEvents();
    }

    @Test
    void searchEvents_WithPartialIdThatMatches_ShouldReturnEvent() throws Exception {
        // Arrange
        String partialId = testEventId.toString().substring(0, 8);
        when(eventService.getAllEvents()).thenReturn(Collections.singletonList(testRegisteredEvent));

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search")
                        .param("id", partialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(testEventId.toString()));

        verify(eventService).getAllEvents();
    }

    @Test
    void searchEvents_WithoutParameters_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Please provide 'id' or 'originalId' parameter"));

        verify(eventService, never()).getEventById(any());
        verify(eventService, never()).getEventByOriginalId(any());
        verify(eventService, never()).getAllEvents();
    }

    @Test
    void searchEvents_WithBothParameters_ShouldPrioritizeId() throws Exception {
        // Arrange
        when(eventService.getEventById(testEventId)).thenReturn(testRegisteredEvent);

        // Act & Assert
        mockMvc.perform(get("/api/registry/events/search")
                        .param("id", testEventId.toString())
                        .param("originalId", testOriginalEventId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testEventId.toString()));

        verify(eventService).getEventById(testEventId);
        verify(eventService, never()).getEventByOriginalId(any());
    }

    @Test
    void searchEvents_WithNotFoundId_ShouldReturnNotFound() throws Exception {

        UUID nonExistentId = UUID.randomUUID();

        when(eventService.getEventById(nonExistentId)).thenReturn(null);

        List<RegisteredEvent> allEvents = Arrays.asList(testRegisteredEvent);
        when(eventService.getAllEvents()).thenReturn(allEvents);

        mockMvc.perform(get("/api/registry/events/search")
                        .param("id", nonExistentId.toString()))
                .andExpect(status().isNotFound());

        verify(eventService).getEventById(nonExistentId);
        verify(eventService).getAllEvents();
    }
}