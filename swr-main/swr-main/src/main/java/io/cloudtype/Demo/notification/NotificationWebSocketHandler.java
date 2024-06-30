package io.cloudtype.Demo.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.jwt.JWTUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdToUserIdMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartBeatExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JWTUtil jwtUtil;

    public NotificationWebSocketHandler(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void init() {
        heartBeatExecutor = Executors.newScheduledThreadPool(1);
        heartBeatExecutor.scheduleAtFixedRate(this::sendHeartBeat, 0, 55, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        heartBeatExecutor.shutdown();
    }

    private void sendHeartBeat() {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error sending ping message to session " + userId + ": " + e.getMessage());
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        // 초기에는 세션을 등록하지 않습니다.
        System.out.println("WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        Map<String, String> messagePayload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = messagePayload.get("type");

        if ("Authorization".equals(type)) {
            String token = messagePayload.get("token");
            if (token != null && !token.isEmpty()) {
                try {
                    token = token.split(" ")[1];
                    String userId = jwtUtil.getUsername(token, 1);
                    if (userId != null) {
                        userSessions.put(userId, session);
                        sessionIdToUserIdMap.put(session.getId(), userId);
                        System.out.println("WebSocket authorized userId: " + userId);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "success"))));
                        return;
                    }
                } catch (Exception e) {
                    log.error("Authorization error for token: " + token, e);
                }
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "fail"))));
            session.close(CloseStatus.BAD_DATA);
        } else {
            System.out.println("Received notification message: " + message.getPayload());
            // Handle other message types here
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        System.out.println("Notification WebSocket connection closed: " + session.getId() + " - " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
        }
        System.err.println("WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    public void sendNotificationToUser(String userId, String notificationMessage) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, String> notificationPayload = Map.of(
                        "type", "notification",
                        "message", notificationMessage
                );
                String jsonResponse = objectMapper.writeValueAsString(notificationPayload);
                session.sendMessage(new TextMessage(jsonResponse));
                System.out.println("Sent notification message to: " + session.getId() + " - " + jsonResponse);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error sending notification message to: " + session.getId() + " - " + e.getMessage());
            }
        } else {
            System.err.println("No active session found for user: " + userId);
        }
    }

    public boolean isUserConnected(String userId) {
        return userSessions.containsKey(userId);
    }
}