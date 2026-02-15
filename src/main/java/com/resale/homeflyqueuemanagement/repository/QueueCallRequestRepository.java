package com.resale.homeflyqueuemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.resale.homeflyqueuemanagement.model.CallStatus;
import com.resale.homeflyqueuemanagement.model.QueueCallRequest;

import java.util.List;

public interface QueueCallRequestRepository extends JpaRepository<QueueCallRequest, Long> {
    List<QueueCallRequest> findAll(); // e.g., WAITING
    boolean existsByAssignedUserIdAndStatusIn(Integer assignedUserId, List<CallStatus> statuses);

}


