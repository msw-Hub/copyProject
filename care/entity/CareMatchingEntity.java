package io.cloudtype.Demo.care.entity;

import io.cloudtype.Demo.mypage.pet.PetEntity;
import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CareMatchingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "care_post_id")  // 외래 키 컬럼 이름 지정
    private CarePostEntity carePost;  // 연관된 UserEntity 객체

    @ManyToOne
    @JoinColumn(name = "pet_entity_id")  // 외래 키 컬럼 이름 지정
    private PetEntity pet;  // 연관된 PetEntity 객체

    @ManyToOne
    @JoinColumn(name = "owner_id")    // 외래 키 컬럼 이름 지정
    private UserEntity owner;  // 산책 동행자

    private int status;  // 0: 신청중(매칭전), 1: 매칭완료 2:돌봄시작 3:돌봄정상종료 4:문제발생(비정상종료)

    private int amount;  // 총 돌봄비용

    private LocalDateTime applyDate;  // 신청일

    private LocalDateTime reservationStartDate;
    private LocalDateTime reservationEndDate;

    private LocalDateTime startDate;

    private String requestMessage;  // 요청 메시지

    @PrePersist
    public void prePersist() {
        this.status = 0;  // 초기 상태 설정
    }
}
