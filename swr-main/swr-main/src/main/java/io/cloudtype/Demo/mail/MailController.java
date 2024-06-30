package io.cloudtype.Demo.mail;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {
    private final MailSendService mailService;

    @PostMapping("/send")
    public ResponseEntity<?> mailSend(@RequestBody @Valid EmailRequestDto emailDto
    ) {
        try {
            log.info("emailDto: {}", emailDto);
            String authNum = mailService.joinEmail(emailDto.getEmail());
            mailService.saveAuthNum(emailDto.getEmail(), authNum);
            return ResponseEntity.ok("이메일 인증 번호 전송 성공");
        }catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/check")
    public ResponseEntity<?> AuthCheck(@RequestBody @Valid EmailCheckDto emailCheckDto
    ){
        try {
            Boolean Checked = mailService.checkAuthNum(emailCheckDto.getEmail(), emailCheckDto.getAuthNum());
            if(Checked){
                return ResponseEntity.ok("인증 성공");
            }else{
                return ResponseEntity.ok("인증 실패");
            }
        }catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}