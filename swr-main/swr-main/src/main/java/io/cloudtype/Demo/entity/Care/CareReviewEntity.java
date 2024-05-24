package io.cloudtype.Demo.entity.Care;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class CareReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "care_recode_id")  // 외래 키 컬럼 이름 지정
    private CareRecodeEntity careRecode;  // 연관된 CareRecodeEntity 객체

    private double rating;          //평점
    private String content;         //리뷰 내용
    private LocalDateTime createDate;      //리뷰 작성일
    private String imgUrl;          //이미지 URL

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
    }
}