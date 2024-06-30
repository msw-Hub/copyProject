package io.cloudtype.Demo.walk.entity;

import io.cloudtype.Demo.mypage.pet.PetEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class WalkMatchingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "user_entity_id")  // 외래 키 컬럼 이름 지정
    private UserEntity user;  // 연관된 UserEntity 객체

    @OneToOne
    @JoinColumn(name = "pet_entity_id")  // 외래 키 컬럼 이름 지정
    private PetEntity pet;  // 연관된 PetEntity 객체
    private int petId;  // 펫 ID - 첫요청 받을때만 쓰임

    private int walkTime;
    private LocalDateTime startTime;
    private LocalDateTime createDate;
    private double latitude;
    private double longitude;
    private String address;           //도로명주소
    private String detailAddress;
    private String title;
    private String content;
    private int amount;  //산책비용
    private int status;  // 0: 신청중(매칭전), 1: 매칭완료 산책진행중, 2: 산책완료

    @OneToOne
    @JoinColumn(name = "walker_id")    // 외래 키 컬럼 이름 지정
    private UserEntity walker;  // 산책 동행자

    @PrePersist
    public void prePersist() {
        this.status = 0;  // 초기 상태 설정
        this.walker = null;  // 산책 동행자 초기화
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
    }
}
