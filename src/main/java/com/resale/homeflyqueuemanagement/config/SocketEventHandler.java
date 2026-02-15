package com.resale.homeflyqueuemanagement.config;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.springframework.stereotype.Component;
import com.resale.homeflyqueuemanagement.components.dto.UserStatus;
import com.resale.homeflyqueuemanagement.components.service.UserStatusService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SocketEventHandler {

    private final UserStatusService userStatusService;

/*    public SocketEventHandler(SocketIOServer server, UserStatusService userStatusService) {
        this.userStatusService = userStatusService;

        // When a user connects
        server.addConnectListener(onUserConnect());

        // When a user disconnects
        server.addDisconnectListener(onUserDisconnect());

        // When user manually changes status
        server.addEventListener("status:change", String.class, (client, status, ack) -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {
                UserStatus newStatus = UserStatus.valueOf(status);
                userStatusService.updateStatus(userId, newStatus);

                System.out.println("EVENT: User " + userId + " -> " + newStatus);
            }
        });
    }*/

    // Keep track of connected clients
    private final Map<String, SocketIOClient> connectedUsers = Collections.synchronizedMap(new HashMap<>());

    public SocketEventHandler(SocketIOServer server, UserStatusService userStatusService) {
        this.userStatusService = userStatusService;

        // ================= CONNECT =================
        server.addConnectListener(client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {

                connectedUsers.put(userId, client);

                String room = "user_" + userId;
                client.joinRoom(room);

                userStatusService.updateStatus(userId, UserStatus.ONLINE);

                System.out.println("USER " + userId + " -> ONLINE");
                printOnlineUsers();
            }
        });

        // ================= DISCONNECT =================
        server.addDisconnectListener(client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {
                connectedUsers.remove(userId);
                userStatusService.updateStatus(userId, UserStatus.OFFLINE);

                System.out.println("USER " + userId + " -> OFFLINE");
                System.out.println("User disconnected: " + userId);

                printOnlineUsers();
            }
        });

        // ================= CUSTOM EVENT =================
        server.addEventListener("status:change", String.class, (client, status, ack) -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {
                userStatusService.updateStatus(userId, UserStatus.valueOf(status));
                System.out.println("User " + userId + " status changed to " + status);
            }
        });
    }
    public void emitToUser(String userId, String event, Object data) {
        SocketIOClient client = connectedUsers.get(userId);
        if (client != null) {
            System.out.println("Entered Client != null ");
            client.sendEvent(event, data);
        }
        System.out.println("After Client != null ");
    }

    public void printOnlineUsers() {
        Set<String> users = connectedUsers.keySet();
        System.out.println("Currently connected users: " + users);
    }

    // Optional: get online users programmatically
    public Set<String> getOnlineUsers() {
        return connectedUsers.keySet();
    }
    private ConnectListener onUserConnect() {
        return client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {
                userStatusService.updateStatus(userId, UserStatus.ONLINE);
            }
        };
    }

    private DisconnectListener onUserDisconnect() {
        return client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null) {
                userStatusService.updateStatus(userId, UserStatus.OFFLINE);
            }
        };
    }
}


