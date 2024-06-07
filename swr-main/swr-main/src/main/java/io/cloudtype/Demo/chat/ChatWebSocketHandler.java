package io.cloudtype.Demo.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.chat.entity.ChatMessageEntity;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


// 웹소켓 연결 수립, 메시지 처리, 연결 종료 시 동작 정의
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatRoomService chatRoomService;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartBeatExecutor;

    @Autowired
    public ChatWebSocketHandler(ChatRoomService chatRoomService, ObjectMapper objectMapper
    ) {
        this.chatRoomService = chatRoomService;
        this.objectMapper = objectMapper;
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
        sessions.forEach((id, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));
                } catch (Exception e) {
                    log.error("Error sending ping message to session " + id, e);
                }
            }
        });
    }
    // chatwebsocket이 연결 되었을 떄 세션을 추가
    @Override
    public void afterConnectionEstablished(@NotNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("Chat WebSocket connection established: session.getId() = " + session.getId());
    }
    @Override
    protected void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws Exception {
        try {
            Map<String, String> payload = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, String>>() {});
            String type = payload.get("type");
            int roomId = Integer.parseInt(payload.get("roomId"));
            String sender = payload.get("sender");
            String msgContent = payload.get("message");

            if (roomId == 0 || sender == null) {
                log.error("필수 정보가 누락되었습니다");
                return;
            }
            if ("join".equals(type)) {
                handleJoinRoom(session, roomId, sender);
            } else if ("message".equals(type)) {
                if (msgContent == null) {
                    log.error("필수 정보가 누락되었습니다");
                    return;
                }
                handleChatMessage(roomId, sender, msgContent);
            } else {
                log.error("전송 타입이 잘못되었습니다: {}", type);
            }
        } catch (Exception e) {
            log.error("에러 발생: {}", e.getMessage());
            session.close(CloseStatus.SERVER_ERROR);
        }
    }
    //채팅방 입장
    private void handleJoinRoom(WebSocketSession session, int roomId, String sender) {
        chatRoomService.addUserToRoom(roomId, session.getId());
        sessionRoomMap.put(session.getId(), String.valueOf(roomId));
        log.info("User {} joined room {}", sender, roomId);

        String response = chatRoomService.getUsersInRoom(roomId);
        log.info("Sending user list to user {}: {}", sender, response);
    }

    // 채팅방 입장 후 채팅 메시지 전송할 때 동작
    private void handleChatMessage(int roomId, String sender, String msgContent) {
        ChatMessageEntity chatMessage = chatRoomService.saveChatMessage(roomId, sender, msgContent);
        if (chatMessage == null) {
            log.error("Failed to save chat message");
            return;
        }

        // Set<String> : 중복저장이 안되는 집합
        Set<String> users = chatRoomService.getChatRoom(roomId);
        if (users == null) {
            log.error("No users found for roomId: " + roomId);
            return;
        }
        log.info("Sending message to {} users in room {}", users.size(), roomId);

        for (String user : users) {
            WebSocketSession userSession = sessions.get(user);
            if (userSession != null && userSession.isOpen()) {
                try {
                    Map<String, String> messagePayload = new HashMap<>();
                    messagePayload.put("type", "message");
                    messagePayload.put("roomId", String.valueOf(roomId));
                    messagePayload.put("sender", sender);
                    messagePayload.put("message", msgContent);
                    messagePayload.put("sendTime", chatMessage.getSendTime().toString());

                    String messageJson = objectMapper.writeValueAsString(messagePayload);
                    userSession.sendMessage(new TextMessage(messageJson));
                    log.info("Message sent to user {}", user);
                } catch (Exception e) {
                    log.error("Error sending message to user " + user, e);
                }
            } else {
                log.error("Session not found or not open for user {}", user);
            }
        }
    }

    @Override
    public void handleTransportError(@NotNull WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: " + exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR); // 에러 발생 시 세션을 닫습니다
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        String roomId = sessionRoomMap.remove(session.getId());
        if (roomId != null) {
            chatRoomService.removeUserFromRoom(Integer.parseInt(roomId), session.getId());
        }
        log.info("Chat WebSocket connection closed: " + session.getId() + " with status " + status);
    }
}