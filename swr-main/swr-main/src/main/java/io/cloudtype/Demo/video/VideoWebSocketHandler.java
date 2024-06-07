package io.cloudtype.Demo.video;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class VideoWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("Video WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // 메시지 파싱 로직 추가 필요
        log.info("Received video message: " + payload);

        // 예시: 특정 조건에 따라 메시지를 브로드캐스트
        for (WebSocketSession userSession : sessions.values()) {
            if (userSession.isOpen()) {
                userSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("Video WebSocket connection closed: " + session.getId() + " with status " + status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: " + exception.getMessage());
        sessions.remove(session.getId());
        session.close(CloseStatus.SERVER_ERROR);
    }
}
