package io.cloudtype.Demo.care.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CarePostListDTO {

    private int carePostId;
    private String caregiverNickname;
    private String caregiverProfileImage;

    private double caregiverRating;
    private int caregiverReviewCount;

    private String title;

    private String administrativeAddress1;      //행정주소1 - 시도
    private String administrativeAddress2;      //행정주소2 - 시군구
    private double distance;                    //집으로부터의 거리

    private List<String> careImages;  // 돌봄 이미지
}
