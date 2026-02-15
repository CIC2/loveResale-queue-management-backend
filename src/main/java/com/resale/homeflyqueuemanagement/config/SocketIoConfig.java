package com.resale.homeflyqueuemanagement.config;

import org.springframework.context.annotation.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.annotation.Bean;

@Configuration
public class SocketIoConfig {

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration socketConfig =
                new com.corundumstudio.socketio.Configuration();
        socketConfig.setHostname("0.0.0.0");
        socketConfig.setPort(9092);

        return new SocketIOServer(socketConfig);
    }
}


