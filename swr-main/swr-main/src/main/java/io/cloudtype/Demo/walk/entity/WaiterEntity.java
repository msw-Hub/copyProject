package io.cloudtype.Demo.walk.entity;

import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class WaiterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "walk_matching_id")  // 외래 키 컬럼 이름 지정
    private WalkMatchingEntity walkMatching;  // 연관된 WalkMatchingEntity 객체

    @ManyToOne
    @JoinColumn(name = "writer_id")  // 외래 키 컬럼 이름 지정
    private UserEntity writer;  // 연관된 UserEntity 객체

    @ManyToOne
    @JoinColumn(name = "waiter_id")  // 외래 키 컬럼 이름 지정
    private UserEntity waiter;  // 연관된 UserEntity 객체

}
