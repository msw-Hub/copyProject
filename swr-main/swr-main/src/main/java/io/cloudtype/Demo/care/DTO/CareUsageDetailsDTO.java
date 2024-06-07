package io.cloudtype.Demo.care.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CareUsageDetailsDTO {
    private int careRecodeId;
    private String userNickname;
    private String userImage;
    private String caregiverNickname;
    private String caregiverImage;

    private String petName;
    private String petImage;
    private String petGender;
    private String petSpecies;
    private int petBirthYear;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private double latitude;
    private double longitude;
    private String administrativeAddress1;
    private String administrativeAddress2;
    private String streetAddress;
    private String detailAddress;

    private String title;
    private String content;

    private int status;
    private String reason;

    private int amount;
}
