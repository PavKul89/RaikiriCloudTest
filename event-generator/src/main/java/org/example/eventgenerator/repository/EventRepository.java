package org.example.eventgenerator.repository;

import org.example.eventgenerator.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    long countByIsProcessed(Boolean isProcessed);

    List<Event> findByIsProcessedTrue();

    List<Event> findByIsProcessedFalse();
}
