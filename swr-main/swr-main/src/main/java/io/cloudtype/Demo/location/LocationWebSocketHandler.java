package io.cloudtype.Demo.location;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionIdToUserIdMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userLocationMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> userTasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartBeatExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JWTUtil jwtUtil;
    private final LocationService locationService;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    public LocationWebSocketHandler(JWTUtil jwtUtil, LocationService locationService) {
        this.locationService = locationService;
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
        executorService.shutdown();
    }

    private void sendHeartBeat() {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
                } catch (Exception e) {
                    log.error("Error sending ping message to session {}: {}", userId, e.getMessage(), e);
                }
            }
        });
    }

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) {
        log.info("Location WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, @NotNull TextMessage message) throws Exception {
        Map<String, String> messagePayload = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        String type = messagePayload.get("type");

        if ("Authorization".equals(type)) {
            String token = messagePayload.get("token");
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    token = token.split(" ")[1];
                    String userName = jwtUtil.getUsername(token, 1);
                    if (userName != null) {
                        userSessions.put(userName, session);
                        sessionIdToUserIdMap.put(session.getId(), userName);
                        log.info("Location socket connected userId: {}", userName);
                        startPeriodicTask(userName);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "success"))));
                        return;
                    }
                } catch (Exception e) {
                    log.error("Authorization error for token: " + token, e);
                }
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "Authorization", "status", "fail"))));
            session.close(CloseStatus.BAD_DATA);
        } else if ("location".equals(type)) {
            String senderId = sessionIdToUserIdMap.get(session.getId());
            if (senderId != null) {
                log.info("Received location message from senderId: {}", senderId);
                String latitude = messagePayload.get("latitude");
                String longitude = messagePayload.get("longitude");

                if (latitude != null && longitude != null) {
                    String locationJson = objectMapper.writeValueAsString(Map.of("latitude", latitude, "longitude", longitude));
                    userLocationMap.put(senderId, locationJson);
                } else {
                    log.warn("Received location message with missing latitude or longitude: {}", message.getPayload());
                }
            } else {
                log.warn("Received location message from unknown session: {}", session.getId());
            }
        } else {
            log.warn("Received unknown message: {}", message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(@NotNull WebSocketSession session, @NotNull CloseStatus status) {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            userLocationMap.remove(userId);
            stopPeriodicTask(userId);
        }
        log.info("Location WebSocket connection closed: {} - {}", session.getId(), status.getReason());
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, @NotNull Throwable exception) {
        String userId = sessionIdToUserIdMap.remove(session.getId());
        if (userId != null) {
            userSessions.remove(userId);
            userLocationMap.remove(userId);
            stopPeriodicTask(userId);
        }
        log.error("WebSocket transport error: {}", exception.getMessage(), exception);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception e) {
            log.error("Error closing WebSocket session: {}", e.getMessage(), e);
        }
    }

    private void startPeriodicTask(String userName) {
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() -> {
            try {
                String receiver = locationService.checkCondition(userName);
                if (receiver != null) {
                    sendLatestLocation(userName, receiver);
                }
            } catch (Exception e) {
                log.error("Error checking condition or sending location: {}", e.getMessage(), e);
            }
        }, 0, 10, TimeUnit.SECONDS);
        userTasks.put(userName, scheduledFuture);
    }

    private void stopPeriodicTask(String userName) {
        ScheduledFuture<?> scheduledFuture = userTasks.remove(userName);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void sendLatestLocation(String senderId, String receiverId) {
        String location = userLocationMap.get(senderId);
        if (location != null) {
            WebSocketSession session = userSessions.get(receiverId);
            if (session != null && session.isOpen()) {
                try {
                    Map<String, String> locationMap = objectMapper.readValue(location, new TypeReference<>() {});
                    String latitude = locationMap.get("latitude");
                    String longitude = locationMap.get("longitude");

                    Map<String, String> locationPayload = Map.of(
                            "type", "location",
                            "latitude", latitude,
                            "longitude", longitude
                    );
                    String jsonResponse = objectMapper.writeValueAsString(locationPayload);
                    session.sendMessage(new TextMessage(jsonResponse));
                    log.info("Sent location message to user: {} - {}", receiverId, jsonResponse);
                } catch (Exception e) {
                    log.error("Error sending location message to user: {} - {}", receiverId, e.getMessage(), e);
                }
            }
        }
    }
}