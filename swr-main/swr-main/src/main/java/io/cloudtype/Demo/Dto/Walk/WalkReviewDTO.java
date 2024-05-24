package io.cloudtype.Demo.Dto.Walk;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WalkReviewDTO {
    private int id;
    private int walkRecodeId;
    private String userNickname;
    private String walkerNickname;
    private String content;
    private double rating;
    private LocalDateTime reviewDate;
    private int walkTime;
    private String imgUrl;
}
