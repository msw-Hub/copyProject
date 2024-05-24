package io.cloudtype.Demo.Dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JoinDTO {

    private String username;        //이메일
    private String password;        //비밀번호

    private String name;            //실명 (카카오로그인시, 카카오에서 받아온 실명)
    private String gender;          //성별 (카카오로그인시, 카카오에서 받아온 성별)

}