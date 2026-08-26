package com.sb.repository;

import com.sb.model.OutboxEvent;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxRepository
        extends MongoRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}