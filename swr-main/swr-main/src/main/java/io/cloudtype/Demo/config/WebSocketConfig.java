package io.cloudtype.Demo.config;

import io.cloudtype.Demo.chat.ChatWebSocketHandler;
import io.cloudtype.Demo.location.LocationWebSocketHandler;
import io.cloudtype.Demo.notification.NotificationWebSocketHandler;
import io.cloudtype.Demo.video.VideoWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final VideoWebSocketHandler videoWebSocketHandler;
    private final LocationWebSocketHandler locationWebSocketHandler;
    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                           ChatWebSocketHandler chatWebSocketHandler,
                           VideoWebSocketHandler videoWebSocketHandler,
                            LocationWebSocketHandler locationWebSocketHandler
    ) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.videoWebSocketHandler = videoWebSocketHandler;
        this.locationWebSocketHandler = locationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/notifications").setAllowedOrigins("*");
        registry.addHandler(chatWebSocketHandler, "/chat").setAllowedOrigins("*");
        registry.addHandler(videoWebSocketHandler, "/video").setAllowedOrigins("*");
        registry.addHandler(locationWebSocketHandler, "/location").setAllowedOrigins("*");
        log.info("WebSocket handlers registered: /notifications, /chat and /video");
        //System.out.println("WebSocket handlers registered: /notifications, /chat and /video");
    }
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(1800000L);  // 30 minutes
        return container;
    }
}