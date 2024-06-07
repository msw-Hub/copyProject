package io.cloudtype.Demo.community.entity;

import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "community_board_id")  // 게시글 ID 외래 키
    private CommunityBoardEntity communityBoard;

    @ManyToOne
    @JoinColumn(name = "user_id")  // 유저 ID 외래 키
    private UserEntity user;

    @Column(nullable = false)
    private String content;  // 댓글 내용

    @Column(nullable = false)
    private LocalDateTime createDate;  // 댓글 작성 시간

    private int likeCount;  // 댓글 좋아요 수

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
        this.likeCount = 0;  // 초기 좋아요 수 설정
    }
}
