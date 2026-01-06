package org.example.eventregistry.repository;

import org.example.eventregistry.entity.RegisteredEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface RegisteredEventRepositoryCustom {
    Page<RegisteredEvent> findWithFilters(
            Pageable pageable,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String eventType,
            String serviceName
    );

    List<String> findDistinctEventTypes();
    List<String> findDistinctServiceNames();
}
