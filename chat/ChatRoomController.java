package io.cloudtype.Demo.chat;

import io.cloudtype.Demo.chat.DTO.ChatHistoryResponseDTO;
import io.cloudtype.Demo.chat.DTO.ChatMessageDTO;
import io.cloudtype.Demo.chat.DTO.ChatRoomDTO;
import io.cloudtype.Demo.chat.entity.ChatMessageEntity;
import io.cloudtype.Demo.chat.entity.ChatRoomEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.PartnerEntity;
import io.cloudtype.Demo.mypage.user.PartnerRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 채팅방 생성/조회 API 제공
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    //내가 속한 채팅방 목록 조회
    @GetMapping("/room/list")
    public ResponseEntity<?> getRoomList(@RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<ChatRoomDTO> response = chatRoomService.getRoomList(accessToken);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // roomId를 통한 채팅 내역 불러오기 : roomId를 파라미터로 받기에는 상단바에 번호가 보임
    @GetMapping("/room/history")
    public ResponseEntity<?> getChatHistory(@RequestParam("roomId") int roomId,
                                            @RequestHeader("Authorization") String accessToken
    ) {
        try {
            List<ChatMessageDTO> chatMessageDTOS = chatRoomService.getRoomDetails(accessToken, roomId);
            return ResponseEntity.ok(chatMessageDTOS);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}