package io.cloudtype.Demo.walk.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WalkUsageSimpleDTO {
    private int walkRecodeId;
    private String userNickname;
    private String userImage;
    private String walkerNickname;
    private String walkerImage;

    private String petName;

    private int walkTime;
    private LocalDateTime endTime;
    
    private int amount;
    private boolean review;

    private int status;
}
