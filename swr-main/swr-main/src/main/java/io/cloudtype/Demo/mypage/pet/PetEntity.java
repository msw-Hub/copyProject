package io.cloudtype.Demo.mypage.pet;

import io.cloudtype.Demo.mypage.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "user_id")  // 주인의 ID
    private UserEntity owner;
    private String petName;
    private String petImage;
    private int birthYear;
    private String gender;

    private String species;
    private double weight;
    private boolean neutering; // 중성화 여부
    private String animalHospital; // 담당 동물 병원
    private String vaccination; // 백신 접종 여부
    private String etc; // 기타 정보

}
