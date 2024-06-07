package io.cloudtype.Demo.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.chat.DTO.ChatMessageDTO;
import io.cloudtype.Demo.chat.DTO.ChatRoomDTO;
import io.cloudtype.Demo.chat.entity.ChatMessageEntity;
import io.cloudtype.Demo.chat.entity.ChatRoomEntity;
import io.cloudtype.Demo.chat.repository.ChatMessageRepository;
import io.cloudtype.Demo.chat.repository.ChatRoomRepository;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.PartnerEntity;
import io.cloudtype.Demo.mypage.user.PartnerRepository;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.cloudtype.Demo.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ConcurrentHashMap<String, Set<String>> chatRooms = new ConcurrentHashMap<>();
    private final JWTUtil jwtUtil;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final NotificationService notificationService;

    @Autowired
    public ChatRoomService(ChatRoomRepository chatRoomRepository, JWTUtil jwtUtil, ChatMessageRepository chatMessageRepository, UserRepository userRepository,
                           PartnerRepository partnerRepository, NotificationService notificationService
    ) {
        this.chatRoomRepository = chatRoomRepository;
        this.jwtUtil = jwtUtil;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.notificationService = notificationService;
    }

    // 채팅방 생성 요청 처리
    @Transactional
    public void createChatRoom(UserEntity user, UserEntity partner) {
        log.info("Creating chat room");
        // 전달받은 값들을 Entity 형식에 저장
        ChatRoomEntity chatRoom = new ChatRoomEntity(user, partner);
        // 리포지토리에 접근하여 방금전 입력 받은 채팅방 정보 db에 저장
        chatRoomRepository.save(chatRoom);
        log.info("Chat room created" + chatRoom.getRoomId());

        // 채팅방 생성 시, 두 사용자에게 알림을 보냄
        if (notificationService.isUserConnected(user.getUsername())) {
            notificationService.notifyUser(user.getUsername(), "채팅방이 생성되었습니다.");
        } else {
            log.warn("User " + user.getUsername() + " is not connected. Notification not sent.");
        }
        if (notificationService.isUserConnected(partner.getUsername())) {
            notificationService.notifyUser(partner.getUsername(), "채팅방이 생성되었습니다.");
        }else {
            log.warn("Partner " + partner.getUsername() + " is not connected. Notification not sent.");
        }
    }
    // 해당 roomId에 들어있는 유저집합을 반환 - 해당 채팅방에 참여중인 사람을 조회할 수 있음 - 입장시 확인용으로 사용가능?
    public String getUsersInRoom(int roomId) {
        Set<String> usersInRoom = chatRooms.getOrDefault(String.valueOf(roomId), new ConcurrentSkipListSet<>());
        Map<String, String> userMap = new HashMap<>();
        Iterator<String> iterator = usersInRoom.iterator();

        if (iterator.hasNext()) {
            userMap.put("user", iterator.next());
        } else {
            log.error("No user found in room {}", roomId);
        }

        if (iterator.hasNext()) {
            userMap.put("partner", iterator.next());
        } else {
            log.error("No partner found in room {}", roomId);
        }

        // ObjectMapper를 사용하여 맵을 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(userMap);
            System.out.println(json); // {"A":"user1","B":"user2"}
            return json;
        } catch (Exception e) {
            log.error("Error converting user map to JSON", e);
            throw new IllegalArgumentException("사용자 조회 실패");
            // 예외 처리 로직
        }
    }
    // roomId로 채팅방 정보 가져오기
    public Set<String> getChatRoom(int roomId) {
        if (roomId == 0) {
            throw new IllegalArgumentException("roomId cannot be null");
        }
        return chatRooms.get(String.valueOf(roomId));
    }

    // 채팅방에 이용자 추가하기>>>????
    public void addUserToRoom(int roomId, String sessionId) {
        chatRooms.computeIfAbsent(String.valueOf(roomId), k -> new ConcurrentSkipListSet<>()).add(sessionId);
    }

    // 채팅방에서 이용자 내보내기
    public void removeUserFromRoom(int roomId, String sessionId) {
        Set<String> users = chatRooms.get(String.valueOf(roomId));
        if (users != null) {
            users.remove(sessionId);
            if (users.isEmpty()) {
                chatRooms.remove(String.valueOf(roomId));
            }
        }
    }

    public void removeUserFromAllRooms(String sessionId) {
        chatRooms.forEach((roomId, users) -> removeUserFromRoom(Integer.parseInt(roomId), sessionId));
    }

    public List<ChatRoomEntity> getAllChatRooms() {
        return chatRoomRepository.findAll();
    }

    @Transactional
    public String getLatestMessageByRoomId(int roomId) {
        ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("Invalid roomId: " + roomId);
        }
        ChatMessageEntity latestMessageEntity = chatMessageRepository.findFirstByChatRoomOrderBySendTimeDesc(chatRoom);
        return latestMessageEntity != null ? latestMessageEntity.getContent() : null;
    }

/*
    내가 짠 부분
 */
    //내가 속한 채팅방 리스트 조회
    public List<ChatRoomDTO> getRoomList(String accessToken) {
        UserEntity user = getUserEntity(accessToken);

        // 현재 사용자가 user로 들어가 있는 채팅방
        List<ChatRoomEntity> userChatRooms = chatRoomRepository.findByUser(user);
        List<ChatRoomDTO> userChatRoomsDTO = userChatRooms.stream()
                .map(entity-> getChatRoomDTO(entity,1))
                .toList();

        // 현재 사용자가 partner로 들어가 있는 채팅방
        List<ChatRoomEntity> partnerChatRooms = chatRoomRepository.findByPartner(user);
        List<ChatRoomDTO> partnerChatRoomsDTO = partnerChatRooms.stream()
                .map(entity-> getChatRoomDTO(entity,0))
                .toList();

        // 두 리스트를 합칩니다.
        List<ChatRoomDTO> combinedChatRooms = new ArrayList<>();
        combinedChatRooms.addAll(userChatRoomsDTO);
        combinedChatRooms.addAll(partnerChatRoomsDTO);

        return combinedChatRooms.stream() // 스트림 생성
                .sorted(Comparator.comparing(ChatRoomDTO::getUpdatedAt).reversed()) // 내림차순 정렬
                .toList();
    }
    private ChatRoomDTO getChatRoomDTO(ChatRoomEntity chatRoom, int role) {
        ChatRoomDTO chatRoomDTO = new ChatRoomDTO();
        chatRoomDTO.setRoomId(chatRoom.getRoomId());
        chatRoomDTO.setUserNickname(chatRoom.getUser().getNickname());
        chatRoomDTO.setPartnerNickname(chatRoom.getPartner().getNickname());
        chatRoomDTO.setUpdatedAt(chatRoom.getUpdatedAt().toString());
        chatRoomDTO.setLatestMessage(getLatestMessageByRoomId(chatRoom.getRoomId()));
        // reverse_user 설정
        if (role == 1) {//user로 조회한 경우 상대편 partner
            chatRoomDTO.setReverse_user(chatRoom.getPartner().getNickname());
            PartnerEntity partnerEntity = partnerRepository.findByUser_Id(chatRoom.getPartner().getId());
            chatRoomDTO.setImage(partnerEntity.getPartnerProfileImage());
        } else {//partner로 조회한 경우 상대편은 user
            chatRoomDTO.setReverse_user(chatRoom.getUser().getNickname());
            chatRoomDTO.setImage(chatRoom.getUser().getProfileImage());
        }
        return chatRoomDTO;
    }
    //내가 속한 채팅방의 채팅 내역 조회
    public List<ChatMessageDTO> getRoomDetails(String accessToken, int roomId) {
        UserEntity user = getUserEntity(accessToken);
        ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId);
        if (chatRoom == null) {
            throw new IllegalArgumentException("해당 roomId에 해당하는 방이 없습니다");
        }
        if (chatRoom.getUser().getId() != user.getId() && chatRoom.getPartner().getId() != user.getId()) {
            throw new IllegalArgumentException("해당 roomId에 참여중인 사용자가 아닙니다");
        }
        //채팅리스트 불러오기
        List<ChatMessageEntity> messages = chatMessageRepository.findByChatRoom_RoomId(roomId);
        List<ChatMessageDTO> response = messages.stream()
                .sorted(Comparator.comparing(ChatMessageEntity::getSendTime))
                .map(ChatMessageDTO::new)
                .toList();
        return response;
    }
    public UserEntity getUserEntity(String accessToken) {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
    //채팅 메시지 저장
    public ChatMessageEntity saveChatMessage(int roomId, String sender, String msgContent) {
        // 들어온 roomId로 채팅방 목록을 조회 - 없으면 다시 반환
        ChatRoomEntity chatRoom = chatRoomRepository.findByRoomId(roomId);
        if (chatRoom == null) {
            log.error("roomId에 해당하는 채팅방이 없습니다");
            return null;
        }
        log.info("chatRoomEntity : {}", chatRoom);
        // 유저 타입 구분 (user : 0, partner : 1)
        UserEntity userEntity = userRepository.findByUsername(sender);
        if (userEntity == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }

        //체크한 유저 타입 구분 (user : 0, walker : 1)
        int userType;
        if (chatRoom.getUser().getId() == userEntity.getId()) {
            userType = 0;
        } else if (chatRoom.getPartner().getId() == userEntity.getId()) {
            userType = 1;
        } else {
            log.error("해당 유저가 채팅방에 속해있지 않습니다");
            return null;
        }
        //채팅방 정보도 업데이트
        chatRoom.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(chatRoom);

        //채팅 내역 db에 저장
        ChatMessageEntity chatMessage = new ChatMessageEntity();
        chatMessage.setChatRoom(chatRoom);
        chatMessage.setUsertype(userType);
        chatMessage.setContent(msgContent);
        chatMessage.setSendTime(LocalDateTime.now());
        chatMessageRepository.save(chatMessage);
        log.info("Received chat message: " + msgContent);

        return chatMessage;
    }
}
