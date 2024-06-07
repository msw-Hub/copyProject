package io.cloudtype.Demo.care.entity;

import io.cloudtype.Demo.mypage.pet.PetEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class CareRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "caregiver_id")  // 외래 키 컬럼 이름 지정
    private UserEntity caregiver;  // 연관된 UserEntity 객체

    @ManyToOne
    @JoinColumn(name = "pet_entity_id")  // 외래 키 컬럼 이름 지정
    private PetEntity pet;  // 연관된 PetEntity 객체

    @ManyToOne
    @JoinColumn(name = "owner_id")    // 외래 키 컬럼 이름 지정
    private UserEntity owner;  // 산책 동행자

    private int status;  //3:돌봄정상종료 4:문제발생(비정상종료)

    private int amount;  // 돌봄비용

    private LocalDateTime reservationStartDate;     //예약 시작일시
    private LocalDateTime reservationEndDate;       //예약 종료일시

    private LocalDateTime startDate;                //돌봄 시작일시
    private LocalDateTime endDate;                   //돌봄 종료일시

    private String requestMessage;

    private boolean review;  //리뷰 작성 여부
    private String reason;  //문제 발생시 사유

    @PrePersist
    public void prePersist() {
        this.review = false;  //리뷰 작성 여부 초기화
    }
}
