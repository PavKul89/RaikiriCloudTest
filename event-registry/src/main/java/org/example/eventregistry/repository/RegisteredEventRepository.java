package org.example.eventregistry.repository;

import org.example.eventregistry.entity.RegisteredEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegisteredEventRepository extends
        JpaRepository<RegisteredEvent, UUID>,
        RegisteredEventRepositoryCustom {

    RegisteredEvent findByOriginalEventId(UUID originalEventId);
}