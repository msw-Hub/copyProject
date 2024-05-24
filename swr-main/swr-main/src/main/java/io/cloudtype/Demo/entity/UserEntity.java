package io.cloudtype.Demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String username;    //이메일
    private String password;    //일반로그인시 비밀번호

    private String role;        //기본역할
    private String refreshToken;    //JWT Refresh Token

    private String profileImage;    //프로필 이미지(일반로그인시, 기본이미지)
    private String name;            //실명 (카카오로그인시, 카카오에서 받아온 실명)
    private String gender;          //성별 (카카오로그인시, 카카오에서 받아온 성별)

    private String nickname;        //닉네임 - 추가 입력사항
    private int pinNumber;          //2차비밀번호 - 추가 입력사항
    private String phoneNumber;     //전화번호 - 추가 입력사항
    private String birth;           //생년월일 - 추가 입력사항

    private int partnership;     //제휴사 - 추가 입력사항

    private boolean kakaoLogin;     //카카오로그인 여부

    //추가사항
    private double walkRating;          //산책서비스 평점
    private int walkReviewCount;            //산책서비스 리뷰수

    private double careRating;          //돌봄서비스 평점
    private int careReviewCount;            //돌봄서비스 리뷰수

    @PrePersist
    public void prePersist() {
        this.kakaoLogin = false;    //카카오로그인 여부 설정
        this.partnership = 0;        //제휴사 설정
        this.walkRating = 0.0;            //산책서비스 평점 설정
        this.walkReviewCount = 0;        //산책서비스 리뷰수 설정
        this.careRating = 0.0;            //돌봄서비스 평점 설정
        this.careReviewCount = 0;        //돌봄서비스 리뷰수 설정
    }
}