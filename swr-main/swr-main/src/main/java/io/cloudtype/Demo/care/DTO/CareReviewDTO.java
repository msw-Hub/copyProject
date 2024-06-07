package io.cloudtype.Demo.care.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CareReviewDTO {
    private int id;
    private int careRecodeId;
    private String userNickname;
    private String caregiverNickname;
    private String content;
    private double rating;
    private LocalDateTime reviewDate;
    private String petSpecies;
    private String imgUrl;
}
