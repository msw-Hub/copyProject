package io.cloudtype.Demo.care.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class CareApplyDetailDTO {

    private int careMatchingId;

    private String petName;
    private String petImage;
    private String petGender;
    private int petBirthYear;
    private String species;
    private double weight;
    private boolean neutering;
    private boolean vaccination;
    private String etc;

    private int amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reservationStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reservationEndDate;

    private String requestMessage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime careStartDate;
}
