package io.cloudtype.Demo.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CareImgEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "care_post_id")  // 외래 키 컬럼 이름 지정
    private CarePostEntity carePost;  // 연관된 CarePostEntity 객체

    private String imgUrl;  // 이미지 URL

    private LocalDateTime uploadDate;  // 업로드 날짜

    @PrePersist
    public void prePersist() {
        this.uploadDate = LocalDateTime.now();  // 현재 시간을 설정
    }
}
