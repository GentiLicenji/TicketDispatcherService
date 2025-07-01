package com.pleased.ticket.dispatcher.server.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_pls")
public class UserEntity {

    @Id
    private UUID userId;

    private String name;

    private String email;

    private String role;

    private String status;

    private String timezone;

    public enum Role {
        ADMIN, AGENT, USER // define based on system
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED
    }
}

