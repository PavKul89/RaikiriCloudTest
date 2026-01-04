package org.example.eventgenerator.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private UUID eventId;
    private String eventType;
    private String serviceName;
    private String payload;
    private LocalDateTime createdAt;
}
