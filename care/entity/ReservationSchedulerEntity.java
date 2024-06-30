package io.cloudtype.Demo.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class ReservationSchedulerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "care_post_id")  // 외래 키 컬럼 이름 지정
    private CarePostEntity carePost;

    private Date unavailableDate;   // 돌봄 불가능 날짜

    private Date reservationDate;   // 예약 날짜
    private int reservations;       // 예약 수

}
