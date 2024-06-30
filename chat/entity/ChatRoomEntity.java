package io.cloudtype.Demo.chat.entity;

import io.cloudtype.Demo.mypage.user.PartnerEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@Setter
public class ChatRoomEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="room_id")
    private int roomId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    private UserEntity partner;

    @UpdateTimestamp
    @Column(nullable = true)
    private LocalDateTime updatedAt;

    // 날짜 형식 변환 메서드
    public String getUpdatedAtFormatted() {
        if (updatedAt != null) {
            return updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return null;
    }
    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    // 기본 생성자
    public ChatRoomEntity() {
    }

    // 생성자 추가
    public ChatRoomEntity(UserEntity user, UserEntity partner) {
        this.user = user;
        this.partner = partner;
    }
}