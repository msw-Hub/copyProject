package io.cloudtype.Demo.entity.Walk;

import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class WalkRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_entity_id")  // 외래 키 컬럼 이름 지정
    private UserEntity user;  // 연관된 UserEntity 객체

    @ManyToOne
    @JoinColumn(name = "pet_entity_id")  // 외래 키 컬럼 이름 지정
    private PetEntity pet;  // 연관된 PetEntity 객체

    private int walkTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createDate;
    private double latitude;
    private double longitude;
    private String address;           //도로명주소
    private String detailAddress;
    private String title;
    private String content;
    private int status;  //3: 산책완료(정상종료) 4:산책미완료(비정상종료)-문제발생
    private String reason;  //산책미완료시 사유
    private int amount;  //산책비용
    private boolean review;  //리뷰 작성 여부

    @ManyToOne
    @JoinColumn(name = "walker_id")    // 외래 키 컬럼 이름 지정
    private UserEntity walker;  // 산책 동행자

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
        this.review = false;  //리뷰 작성 여부 초기화
    }
}
