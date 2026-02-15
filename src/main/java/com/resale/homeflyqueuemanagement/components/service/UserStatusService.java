package com.resale.homeflyqueuemanagement.components.service;

import org.springframework.stereotype.Service;
import com.resale.homeflyqueuemanagement.components.dto.UserStatus;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class UserStatusService {

    private final Map<String, UserStatus> statusMap = new ConcurrentHashMap<>();

    public void updateStatus(String userId, UserStatus status) {
        statusMap.put(userId, status);
        System.out.println("USER " + userId + " -> " + status);
    }

    public UserStatus getStatus(String userId) {
        return statusMap.getOrDefault(userId, UserStatus.OFFLINE);
    }
}


