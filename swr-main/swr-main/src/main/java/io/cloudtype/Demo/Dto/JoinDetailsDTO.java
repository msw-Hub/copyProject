package io.cloudtype.Demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JoinDetailsDTO {
    private String nickname;        //닉네임 - 추가 입력사항
    private int pinNumber;          //2차비밀번호 - 추가 입력사항
    private String phoneNumber;     //전화번호 - 추가 입력사항
    private String birth;           //생년월일 - 추가 입력사항
}
