package io.cloudtype.Demo.walk.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class WalkMatchingDetailsDTO {
    private int id;
    private String userNickname;
    private String userProfile;

    private int petId;
    private int walkTime;
    private LocalDateTime createDate;
    private LocalDateTime startTime; //추가사항
    private double latitude;
    private double longitude;

    private int status; //추가사항
    private String address;           //도로명주소
    private String detailAddress;

    private String title;
    private String content;

    private String walkerNickname; //추가사항
    private String walkerProfile; //추가사항
}