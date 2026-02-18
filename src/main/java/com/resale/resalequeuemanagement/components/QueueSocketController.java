package com.resale.resalequeuemanagement.components;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.resale.resalequeuemanagement.components.service.QueueService;

@RestController
@RequestMapping("/api/socket")
@RequiredArgsConstructor
public class QueueSocketController {

    private final QueueService queueService;

    // ----------------------------------------------------------
    // PROCESS QUEUE USING SOCKET.IO
    // ----------------------------------------------------------
    @PostMapping("/process")
    public ResponseEntity<String> processQueueSocket() {
        queueService.processQueueSocket();
        return ResponseEntity.ok("Queue processed via socket assignment.");
    }
}


