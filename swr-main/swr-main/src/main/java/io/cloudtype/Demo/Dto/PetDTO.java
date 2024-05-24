package io.cloudtype.Demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PetDTO {

    private int id;
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
