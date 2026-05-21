package com.innowise.paymentservice.model;


import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@Document(collection = "outbox_events")
@CompoundIndex(name = "processed_createdAt_idx", def = "{'processed': 1, 'createdAt': 1}")
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {
    
    @Id
    private String id;

    private String aggregateId;   

    private String eventType;     

    private String payload;       

    private boolean processed;    

    @CreatedDate
    private Instant createdAt;
}
