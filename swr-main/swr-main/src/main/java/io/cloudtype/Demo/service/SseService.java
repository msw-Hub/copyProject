package io.cloudtype.Demo.service;

import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    // 클라이언트와의 연결을 관리하기 위한 맵
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    @Autowired
    public SseService(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }
    public SseEmitter handleSse(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        String userId = String.valueOf(user.getId());
        SseEmitter emitter = new SseEmitter();
        sseEmitters.put(userId, emitter); // 연결을 저장
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            sseEmitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE connection timed out for user: {}", userId);
            sseEmitters.remove(userId);
        }); // 연결이 타임아웃되면 제거
        return emitter;
    }

    // 특정 유저에게 메시지를 보내는 메서드
    public void notifyMatch(String userId, String message, int number) {
        SseEmitter emitter = sseEmitters.get(userId);
        if (emitter != null) {
            try {
                if(number==1) emitter.send(SseEmitter.event().name("match").data(message)); // 매칭성공
                else if(number==2) emitter.send(SseEmitter.event().name("apply").data(message)); // 신청자있음 알림
                else if(number==3) emitter.send(SseEmitter.event().name("cancel").data(message)); // 예약취소 알림
                else if(number==4) emitter.send(SseEmitter.event().name("complete").data(message)); // 이용완료 알림
                else if(number==5) emitter.send(SseEmitter.event().name("reject").data(message)); // 신청거절 알림
                else if(number==6) emitter.send(SseEmitter.event().name("start").data(message)); // 이용시작알림
                else if(number==7) emitter.send(SseEmitter.event().name("incomplete").data(message)); // 비정상종료

            } catch (IOException e) {
                log.error("Error sending SSE message to user: {}", userId, e);
                emitter.completeWithError(e); // 오류 발생 시 연결 완료
            }
        } else {
            log.warn("No SSE connection found for user: {}", userId);
        }
    }
}