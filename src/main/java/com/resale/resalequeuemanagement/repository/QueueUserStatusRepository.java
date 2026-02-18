package com.resale.resalequeuemanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.resale.resalequeuemanagement.model.QueueUserStatus;

import java.util.List;

public interface QueueUserStatusRepository extends JpaRepository<QueueUserStatus, Long> {

    List<QueueUserStatus> findAll();
    QueueUserStatus findById(Integer id);

    List<QueueUserStatus> findByIsOnlineTrueAndIsBusyFalse(); // fetch users available for calls
}


