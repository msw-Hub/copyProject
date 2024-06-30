package io.cloudtype.Demo.chat.repository;

import io.cloudtype.Demo.chat.entity.ChatRoomEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    ChatRoomEntity findByRoomId(int roomId);
    List<ChatRoomEntity> findByUser(UserEntity user);
    List<ChatRoomEntity> findByPartner(UserEntity partner);
}