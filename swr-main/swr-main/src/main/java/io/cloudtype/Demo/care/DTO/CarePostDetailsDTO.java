package io.cloudtype.Demo.care.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Setter
@Getter
public class CarePostDetailsDTO {

    private int carePostId;
    private String caregiverNickname;
    private double caregiverRating;
    private int caregiverReviewCount;

    private String title;
    private String content;

    private String administrativeAddress1;      //행정주소1 - 시도
    private String administrativeAddress2;      //행정주소2 - 시군구
    private String streetNameAddress;           //도로명주소
    private String detailAddress;               //상세주소

    private double distance;                    //집으로부터의 거리

    private List<Date> unavailableDate;   // 돌봄 불가능 날짜

    private List<String> careImages;  // 돌봄 이미지

    private String caregiverProfileImage; // 돌봄인 프로필 이미지

}
