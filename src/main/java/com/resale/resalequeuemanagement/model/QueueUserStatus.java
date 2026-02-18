package com.resale.resalequeuemanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueueUserStatus {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Column(nullable = false)
    private boolean isOnline;

    @Column(nullable = false)
    private boolean isBusy;

}


