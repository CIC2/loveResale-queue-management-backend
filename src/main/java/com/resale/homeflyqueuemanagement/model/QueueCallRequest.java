package com.resale.homeflyqueuemanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueueCallRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    private Integer customerId;

    @Enumerated(EnumType.STRING)
    private CallStatus status;

    private LocalDateTime assignedAt;
    private LocalDateTime endedAt;

    @Column(name = "last_assigned_user_id")
    private Integer lastAssignedUserId;

    @Column(name = "attempt_count")
    private Integer attemptCount = 0;

    private Integer assignedUserId;

    private LocalDateTime createdAt;

}


