package com.innowise.paymentservice.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import com.innowise.paymentservice.model.OutboxEvent;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);

    @Query("{ '_id' : { $in : ?0 } }")
    @Update("{ '$set' : { 'processed' : true } }")
    void markAsProcessed(List<String> eventIds);
}
