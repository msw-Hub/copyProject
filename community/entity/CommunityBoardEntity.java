package io.cloudtype.Demo.community.entity;

import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class CommunityBoardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_entity_id")  // 외래 키 컬럼 이름 지정
    private UserEntity user;  // 연관된 UserEntity 객체

    private String title;
    private String content;

    @Column(nullable = false)
    private LocalDateTime createDate;

    private String imgUrl;
    private int likeCount;
    private int commentCount;
    private int viewCount;

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
        this.likeCount = 0;  // 초기 좋아요 수 설정
        this.commentCount = 0;  // 초기 댓글 수 설정
        this.viewCount = 0;  // 초기 조회수 설정
    }
}
