package io.cloudtype.Demo.walk.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WalkUsageDetailsDTO {
    private int walkRecodeId;
    private String userNickname;
    private String userImage;
    private String walkerNickname;
    private String walkerImage;

    private String petName;
    private String petImage;
    private String petGender;
    private String petSpecies;
    private int petBirthYear;

    private int walkTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private double latitude;
    private double longitude;
    private String address;           //도로명주소
    private String detailAddress;

    private String title;
    private String content;
    private int status;
    private String reason;

    private int amount;
}
