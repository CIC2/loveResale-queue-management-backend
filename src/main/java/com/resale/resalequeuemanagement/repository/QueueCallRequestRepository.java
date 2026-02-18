package com.resale.resalequeuemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.resale.resalequeuemanagement.model.CallStatus;
import com.resale.resalequeuemanagement.model.QueueCallRequest;

import java.util.List;

public interface QueueCallRequestRepository extends JpaRepository<QueueCallRequest, Long> {
    List<QueueCallRequest> findAll(); // e.g., WAITING
    boolean existsByAssignedUserIdAndStatusIn(Integer assignedUserId, List<CallStatus> statuses);

}


