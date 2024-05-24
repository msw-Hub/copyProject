package io.cloudtype.Demo.Dto.Care;

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
}
