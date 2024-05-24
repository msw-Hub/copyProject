package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.Dto.JoinDTO;
import io.cloudtype.Demo.Dto.JoinDetailsDTO;
import io.cloudtype.Demo.service.JoinService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/join")
public class JoinController {

    private final JoinService joinService;

    public JoinController(JoinService joinService) {

        this.joinService = joinService;
    }

    @PostMapping("")
    public ResponseEntity<?> joinProcess(@RequestBody JoinDTO joinDTO) {
        boolean result = joinService.joinProcess(joinDTO);
        if (result) {
            return ResponseEntity.ok("회원 가입 성공 >> 로그인 시도");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("사용자가 이미 존재합니다");
        }
    }
    @PostMapping("/emailCheck")
    public ResponseEntity<?> emailCheck(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        if (joinService.usernameCheck(email)==0) {
            return ResponseEntity.ok("사용 가능한 이메일입니다");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 존재하는 계정입니다.");
        }
    }


    //nickname이 비면안됌.
    @PostMapping("/step1")
    public ResponseEntity<?> nicknameCheck(@RequestBody Map<String, String> requestBody) {
        String nickName = requestBody.get("nickname");
        if (joinService.nicknameCheck(nickName)==0) {
            return ResponseEntity.ok("사용 가능한 닉네임입니다 >> /step2 로 이동");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 사용중인 닉네임입니다");
        }
    }
    @PostMapping("/step2")
    public ResponseEntity<?> signUp(@RequestHeader("Authorization") String accessToken,
                                    @RequestBody JoinDetailsDTO joinDetailsDTO) {
        boolean result = joinService.signUp(joinDetailsDTO, accessToken);
        if (result) {
            return ResponseEntity.ok("회원 가입 성공");
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("사용자가 이미 존재합니다");
        }
    }

}
