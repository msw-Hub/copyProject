package io.cloudtype.Demo.entity.Community;

import io.cloudtype.Demo.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class LikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")  // 좋아요를 누른 사용자의 ID
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "community_board_id")  // 좋아요가 눌린 게시글의 ID
    private CommunityBoardEntity communityBoard;

    @ManyToOne
    @JoinColumn(name = "comment_id")  // 좋아요가 눌린 댓글의 ID
    private CommentEntity comment;

}
