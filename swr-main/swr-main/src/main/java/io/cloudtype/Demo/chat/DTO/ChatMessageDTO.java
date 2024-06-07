package io.cloudtype.Demo.chat.DTO;

import io.cloudtype.Demo.chat.entity.ChatMessageEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDTO {

    private String sender;
    private String content;
    private String sendTime;

    public ChatMessageDTO(ChatMessageEntity chatMessageEntity) {
        String nickname;
        if(chatMessageEntity.getUsertype() == 0){
            nickname = chatMessageEntity.getChatRoom().getUser().getNickname();
        }
        else {
            nickname = chatMessageEntity.getChatRoom().getPartner().getNickname();
        }
        this.sender = nickname;
        this.content = chatMessageEntity.getContent();
        this.sendTime = chatMessageEntity.getSendTime().toString(); // 필요에 따라 포맷 변경 가능
    }

}
