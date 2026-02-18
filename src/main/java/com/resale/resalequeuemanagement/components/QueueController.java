package com.resale.resalequeuemanagement.components;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.resale.resalequeuemanagement.components.service.QueueService;
import com.resale.resalequeuemanagement.model.QueueCallRequest;
import com.resale.resalequeuemanagement.model.QueueUserStatus;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class QueueController {
    @Autowired
    private QueueService queueService;


    @GetMapping("assignCall/{userId}")
    public ResponseEntity<?> assignCall(@PathVariable Integer userId){
        QueueCallRequest queueCallRequest = new QueueCallRequest();
        queueCallRequest.setCustomerId(10);
        queueCallRequest.setAssignedUserId(userId);
        return queueService.assignCallToAvailableUser(queueCallRequest);
    }
    // Get all users
    @GetMapping("/users")
    public List<QueueUserStatus> getUsers() {
        return queueService.getAllUsers();
    }

    @GetMapping("/connectedClients")
    public Integer hasConnectedClients() {
        return queueService.hasConnectedClients();
    }

    // Get all calls
    @GetMapping("/calls")
    public List<QueueCallRequest> getCalls() {
        return queueService.getAllCalls();
    }

    // Process the queue (assign calls)
    @PostMapping("/process")
    public void processQueue() {
        queueService.processQueue();
    }

    // Free a user (after finishing call)
//    @PostMapping("/freeUser/{userId}")
//    public void freeUser(@PathVariable Long userId) {
//        queueService.freeUser(userId);
//    }

    @PostMapping("/accept/{callId}")
    public void acceptCall(@PathVariable Long callId) {
        queueService.agentAcceptCall(callId,1);
    }

    @PostMapping("/end/{callId}")
    public void endCall(@PathVariable Long callId) {
        queueService.endCall(callId);
    }
}


