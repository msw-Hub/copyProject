package io.cloudtype.Demo.chat.DTO;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChatHistoryResponseDTO {
    private List<ChatMessageDTO> messages;
}
