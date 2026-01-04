package org.example.eventregistry.repository;

import org.example.eventregistry.entity.RegisteredEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegisteredEventRepository extends JpaRepository<RegisteredEvent, UUID> {

    @Query("SELECT r FROM RegisteredEvent r WHERE r.originalEventId = :originalEventId")
    RegisteredEvent findByOriginalEventId(@Param("originalEventId") UUID originalEventId);

    long count();
}