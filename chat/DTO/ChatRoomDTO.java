package io.cloudtype.Demo.chat.DTO;

import io.cloudtype.Demo.chat.entity.ChatRoomEntity;
import io.cloudtype.Demo.mypage.user.PartnerEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRoomDTO {
    private int roomId;
    private String userNickname; // 닉네임
    private String partnerNickname; // 닉네임
    private String updatedAt;
    private String latestMessage;
    private String reverse_user;    //상대방 닉네임
    private String image;           //상대방 이미지
}