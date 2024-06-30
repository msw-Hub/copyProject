package io.cloudtype.Demo.care.entity;

import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class CarePostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "caregiver_id")  // 외래 키 컬럼 이름 지정
    private UserEntity caregiver;  // 연관된 UserEntity 객체

    private LocalDateTime createDate;
    private String title;
    private String content;

    private String administrativeAddress1;      //행정주소1 - 시도
    private String administrativeAddress2;      //행정주소2 - 시군구
    private String streetNameAddress;           //도로명주소
    private String detailAddress;               //상세주소
    private double latitude;  // 위도
    private double longitude;  // 경도

    private int reservations;  // 예약수 >> 하루 예약수가 2건이 넘어가면 예약 불가능 날짜에 추가

    @OneToMany(mappedBy = "carePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CareImgEntity> careImages = new HashSet<>();

    @OneToMany(mappedBy = "carePost", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ReservationSchedulerEntity> unavailableDates = new HashSet<>();

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
        this.reservations = 0;  // 초기 예약수 설정
    }
}
