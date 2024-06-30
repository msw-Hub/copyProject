package io.cloudtype.Demo.care.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CareUsageSimpleDTO {
    private int careRecodeId;
    private String userNickname;
    private String userImage;
    private String caregiverNickname;
    private String caregiverImage;

    private String petName;

    private LocalDateTime startDate;
    
    private int amount;
    private boolean review;
    private int status;
}
