package io.cloudtype.Demo.community.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CommentDTO {
    private int id;
    private String nickname;
    private String content;
    private LocalDateTime createDate;
    private int likeCount;
}
