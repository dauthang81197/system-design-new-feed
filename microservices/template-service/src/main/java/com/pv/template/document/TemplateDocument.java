package com.pv.template.document;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "template_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TemplateDocument {

    @Id
    private String id;

    @Indexed
    @Field("template_id")
    private String templateId;

    @Field("event_type")
    private String eventType;

    @Field("payload")
    private Object payload;

    @Field("actor_id")
    private String actorId;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
