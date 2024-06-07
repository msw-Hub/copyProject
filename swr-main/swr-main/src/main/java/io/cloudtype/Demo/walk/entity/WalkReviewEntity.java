package io.cloudtype.Demo.walk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class WalkReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "walk_record_id")  // 외래 키 컬럼 이름 지정
    private WalkRecordEntity walkRecord;  // 연관된 WalkRecordEntity 객체

    private double rating;          //평점
    private String content;         //리뷰 내용
    private LocalDateTime createDate;      //리뷰 작성일
    private String imgUrl;          //이미지 URL

    @PrePersist
    public void prePersist() {
        this.createDate = LocalDateTime.now();  // 현재 시간을 설정
    }
}
