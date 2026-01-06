package org.example.eventregistry.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID originalEventId;
    private UUID registeredEventId;
    private String status;
    private LocalDateTime processedAt;
    private String registryServiceName;
}