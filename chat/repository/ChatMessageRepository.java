package io.cloudtype.Demo.chat.repository;

import io.cloudtype.Demo.chat.entity.ChatMessageEntity;
import io.cloudtype.Demo.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByChatRoom_RoomId(int chatRoomId);

    ChatMessageEntity findFirstByChatRoomOrderBySendTimeDesc(ChatRoomEntity chatRoom);

}
