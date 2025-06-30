package com.pleased.ticket.dispatcher.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("project")
public class ProjectEntity {

    @Id
    private UUID projectId;

    private String title;

    private String description;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private String status;

    private String visibility;

    private UUID projectOwnerId;

    public enum ProjectStatus {
        ACTIVE, ARCHIVED
    }

    public enum Visibility {
        PRIVATE, PUBLIC
    }
}

