package io.cloudtype.Demo.mypage.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class PartnerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")  // 유저 ID 외래 키
    private UserEntity user;

    private String partnerProfileImage;    //파트너 프로필 이미지
    private String address;                //거주지
    private String career;                //경력

    private int testScore;            //테스트 점수
    private LocalDate testDate;            //테스트 응시일
    private int testCount;            //테스트 응시횟수

    private LocalDateTime partnerDate;        //파트너 등록일
    private int partnerStep;            //파트너 등록단계
}
