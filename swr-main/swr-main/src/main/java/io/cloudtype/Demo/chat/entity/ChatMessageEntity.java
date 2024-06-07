package io.cloudtype.Demo.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ChatMessageEntity {
    // 기본생성자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // 메시지들(Many)이 채팅룸(One)에 연결
    @ManyToOne
    @JoinColumn(name = "room_id")
    private ChatRoomEntity chatRoom;

    // 보낸사람 구분 : 0 = 사용자, 1 = 파트너 - 채팅 기록에 사용
    @Column(nullable = false)
    private int usertype;

    // 보낸 메시지 내용
    @Column(nullable = false)
    private String content;

    // 메시지를 보낸 시각
    @Column(nullable = false)
    private LocalDateTime sendTime;
}