package com.resale.homeflyqueuemanagement.components.service;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.resale.homeflyqueuemanagement.config.SocketEventHandler;
import com.resale.homeflyqueuemanagement.model.CallStatus;
import com.resale.homeflyqueuemanagement.model.QueueCallRequest;
import com.resale.homeflyqueuemanagement.model.QueueUserStatus;
import com.resale.homeflyqueuemanagement.repository.QueueCallRequestRepository;
import com.resale.homeflyqueuemanagement.repository.QueueUserStatusRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.resale.homeflyqueuemanagement.model.CallStatus.*;

@Slf4j
@Service
public class QueueService {

    private final SocketIOServer socketServer;
    private final QueueUserStatusRepository userRepo;
    private final QueueCallRequestRepository callRepo;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private final SocketEventHandler socketEventHandler;
    private static final List<CallStatus> ACTIVE_STATUSES = List.of(PENDING, RINGING, ON_CALL);
    // prevents concurrency issues

    public QueueService(SocketIOServer socketServer,
                        SocketEventHandler socketEventHandler,
                                QueueUserStatusRepository userRepo,
                        QueueCallRequestRepository callRepo) {
        this.socketServer = socketServer;
        this.socketEventHandler = socketEventHandler ;
        this.userRepo = userRepo;
        this.callRepo = callRepo;
    }
    public Integer hasConnectedClients() {
        return socketServer.getAllClients().size();
    }
    public ResponseEntity<?> assignCallToAvailableUser(QueueCallRequest call) {
        // Get currently online users
        Set<String> onlineUserIds = socketEventHandler.getOnlineUsers();

        if (onlineUserIds.isEmpty()) {
            System.out.println("No users online for call " + call.getId());
            return ResponseEntity.ok("Failed");
        }

        // For simplicity, pick the first available user
        String assignedUserId = onlineUserIds.iterator().next();

        // Optionally: mark user as busy in DB if needed
        // ...

        // Emit event to frontend
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", 2);
        payload.put("customerId", call.getCustomerId());

        socketEventHandler.emitToUser(assignedUserId, "call:received", payload);

        System.out.println("Assigned call " + call.getId() + " → user " + assignedUserId);
        return ResponseEntity.ok("Success");
    }

    public List<QueueUserStatus> getAllUsers() {
        return userRepo.findAll();
    }

    public List<QueueCallRequest> getAllCalls() {
        return callRepo.findAll();
    }

    // ----------------------------------------------------------
    //  PROCESS QUEUE
    // ----------------------------------------------------------

    public void processQueue() {
        log.info("===== PROCESS QUEUE START =====");

        List<QueueUserStatus> availableUsers = userRepo.findAll()
                .stream()
                .filter(u -> u.isOnline() && !u.isBusy())
                .filter(u -> !userHasActiveCalls(u.getId()))
                .collect(Collectors.toList());

        List<QueueCallRequest> pendingCalls = callRepo.findAll()
                .stream()
                .filter(c -> c.getStatus() == PENDING)
                .toList();

        log.info("Available users: {}", availableUsers.stream().map(QueueUserStatus::getId).toList());
        log.info("Pending calls: {}", pendingCalls.stream().map(QueueCallRequest::getId).toList());

        for (QueueCallRequest call : pendingCalls) {

            List<QueueUserStatus> filteredUsers = availableUsers.stream()
                    .filter(u -> !u.getId().equals(call.getLastAssignedUserId()))
                    .toList();

            if (filteredUsers.isEmpty()) {
                log.info("No users available for call {}", call.getId());

                // 🔵 OPTIONAL: notify frontend that the call is still waiting
                socketServer.getRoomOperations("call_" + call.getId())
                        .sendEvent("call:waiting", call.getId());

                continue;
            }

            QueueUserStatus user = filteredUsers.get(0);
            availableUsers.remove(user);

            // 🔵 Notify frontend this user is assigned the call
            socketServer.getRoomOperations("user_" + user.getId())
                    .sendEvent("call:assigned", call);

            assignCallWithTimeout(call, user);
        }

        log.info("===== PROCESS QUEUE END =====");
    }


    // ----------------------------------------------------------
    //  PROCESS QUEUE SOCKET
    // ----------------------------------------------------------
    public void processQueueSocket() {
        List<QueueUserStatus> availableUsers = userRepo.findAll()
                .stream()
                .filter(u -> u.isOnline() && !u.isBusy())
                .filter(u -> !userHasActiveCalls(u.getId()))
                .toList();

        List<QueueCallRequest> pendingCalls = callRepo.findAll()
                .stream()
                .filter(c -> c.getStatus() == PENDING)
                .toList();

        for (QueueCallRequest call : pendingCalls) {

            List<QueueUserStatus> filteredUsers = availableUsers.stream()
                    .filter(u -> !u.getId().equals(call.getLastAssignedUserId()))
                    .toList();

            if (filteredUsers.isEmpty())
                continue;

            QueueUserStatus user = filteredUsers.get(0);
            availableUsers.remove(user);

            user.setBusy(true);
            userRepo.save(user);

            assignCallWithSocket(call, user);
        }
    }


    private boolean userHasActiveCalls(Integer userId) {
        return callRepo.existsByAssignedUserIdAndStatusIn(
                userId,
                ACTIVE_STATUSES
        );
    }
    // =====================================================
    // ASSIGN & TIMEOUT
    // =====================================================

    private void assignCallWithTimeout(QueueCallRequest call, QueueUserStatus user) {
        call.setAssignedUserId(user.getId());
        call.setStatus(RINGING);
        call.setAssignedAt(LocalDateTime.now());
        callRepo.save(call);

        user.setBusy(true);
        userRepo.save(user);

        log.info("Assigned call {} → user {}  (RINGING)", call.getId(), user.getId());

        scheduler.schedule(() -> checkCallTimeout(call.getId()), 30, TimeUnit.SECONDS);
    }
    // =====================================================
    // ASSIGN & TIMEOUT SOCKET
    // =====================================================

    public void assignCallWithSocket(QueueCallRequest call, QueueUserStatus user) {

        // Update DB
        call.setAssignedUserId(user.getId());
        call.setStatus(RINGING);
        call.setAssignedAt(LocalDateTime.now());
        callRepo.save(call);

        // 🔥 Emit event to that specific user
        socketServer.getRoomOperations("user_" + user.getId())
                .sendEvent("call:assigned", call);

        // Start the timeout thread
        startCallTimeout(call, user);
    }

    private void checkCallTimeout(Long callId) {

        QueueCallRequest call = callRepo.findById(callId).orElse(null);
        if (call == null) return;

        if (call.getStatus() != RINGING) {
            log.info("Timeout skipped. Call {} already handled: {}", callId, call.getStatus());
            return;
        }

        handleCallTimeout(call);
    }

    private void handleCallTimeout(QueueCallRequest call) {
        Integer userId = call.getAssignedUserId();
        log.info("TIMEOUT → Call {} user {}", call.getId(), userId);

        // Store last assigned user
        call.setLastAssignedUserId(userId);

        // Mark call as DID_NOT_ANSWER
        call.setStatus(DID_NOT_ANSWER);
        call.setAssignedUserId(null);
        call.setAssignedAt(null);
        callRepo.save(call);

        // Free agent
        if (userId != null) {
            QueueUserStatus user = userRepo.findById(userId);
            if (user != null) {
                user.setBusy(false);
                userRepo.save(user);
                log.info("User {} set to FREE", user.getId());
            }
        }

        // Move call back to pending
        call.setStatus(PENDING);
        callRepo.save(call);
        log.info("Call {} returned to PENDING queue", call.getId());

        processQueue();
    }

    // =====================================================
    // START CALL TIMEOUT SOCKET
    // =====================================================
    private void startCallTimeout(QueueCallRequest call, QueueUserStatus user) {
        new Thread(() -> {
            try {
                Thread.sleep(15000); // 15 seconds timeout
            } catch (InterruptedException ignored) {
            }

            QueueCallRequest updated = callRepo.findById(call.getId()).orElse(null);
            if (updated == null || updated.getStatus() != RINGING) return;

            // Mark as failed
            updated.setStatus(PENDING);
            updated.setLastAssignedUserId(user.getId());
            callRepo.save(updated);

            // Mark user not busy
            user.setBusy(false);
            userRepo.save(user);

            // 🔥 Notify user UI
            socketServer.getRoomOperations("user_" + user.getId())
                    .sendEvent("call:timeout", updated);

        }).start();
    }


    // =====================================================
    // ACCEPT & END CALL
    // =====================================================

    public void agentAcceptCall(Long callId, Integer userId) {
        Optional<QueueCallRequest> callRequestOptional = callRepo.findById(callId);
        QueueCallRequest call;
        if (callRequestOptional.isPresent()) {
            log.error("Call not found: " + callId);
            call = callRequestOptional.get();

            // 🔥 Check that the call is RINGING
            if (call.getStatus() != RINGING) {
                log.error("Call " + callId + " is not in RINGING status.");
            }

            // 🔥 Check that the call is assigned to this user
            if (!userId.equals(call.getAssignedUserId())) {
                log.error("Call " + callId + " is not assigned to user " + userId);
            }

            call.setStatus(ON_CALL);
            callRepo.saveAndFlush(call);

            log.info("User {} accepted call {}", call.getAssignedUserId(), call.getId());
        }
    }

    public void endCall(Long callId) {
        Optional<QueueCallRequest> callRequestOptional = callRepo.findById(callId);
        if (callRequestOptional.isPresent()) {
            QueueCallRequest call = callRequestOptional.get();
            Integer userId = call.getAssignedUserId();

            call.setEndedAt(LocalDateTime.now());
            call.setStatus(FINISHED);
            callRepo.saveAndFlush(call);

            if (userId != null) {
                QueueUserStatus user = userRepo.findById(userId);
                if (user != null) {
                    user.setBusy(false);
                    userRepo.saveAndFlush(user);
                    log.info("User {} freed after finishing call {}", userId, callId);
                }
            }

            processQueue();
        }
    }
}

