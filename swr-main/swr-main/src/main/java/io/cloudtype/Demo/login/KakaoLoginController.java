package io.cloudtype.Demo.login;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("")
@Tag(name = "kakao", description = "카카오 로그인 관련 API")
public class KakaoLoginController {

    @Value("${kakao.client_id}")
    private String client_id;

    @Autowired
    private KakaoService kakaoService;

    @Autowired
    private final JWTUtil jwtUtil;

    private final UserRepository userRepository;

    public KakaoLoginController(JWTUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }


    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) throws IOException {
        Map<String, String> tokens = kakaoService.getTokensFromKakao(client_id, code);
        String accessToken = tokens.get("access_token");

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String profileImage = "https://storage.googleapis.com/swr-bucket/default.jpg"; //기본 이미지로 설정
        String name = (String) userInfo.get("name");
        String gender = (String) userInfo.get("gender");

        //userInfo내용 출력
        log.info(userInfo.toString());

        //데이터베이스에 있는 내용인지 검토
        int isExist = kakaoService.processUser(email);
        //0=회원가입, 1=일반로그인, 2=카카오로그인
        String logMessage = "";
        if(isExist == 0) {
            logMessage = "회원가입";
        }else if(isExist == 1) {
            logMessage = "카카오로그인";
        }else if(isExist == 2) {
            logMessage = "일반로그인";
        }
        log.info(logMessage);

        //jwt 토큰 발행
        String role = "ROLE_USER";
        String jwtAccessToken = jwtUtil.createAccessToken(email, role, 36000000L); // 10시간
        String jwtRefreshToken = jwtUtil.createRefreshToken(email, role, 604800000L); // 1주일
        HttpHeaders headers = new HttpHeaders();

        if(isExist == 0) {  //회원가입
            kakaoService.signUp(email,role,profileImage, name, gender, jwtRefreshToken);
        }else if(isExist == 2) {    //일반로그인하도록 유도
            return ResponseEntity.badRequest().body("일반로그인");   //이후 에러코드정렬
        }else if(isExist == 1) {    //카카오로그인
            UserEntity user = userRepository.findByUsername(email);
            int partner = user.getPartnership();
            String nickname = user.getNickname();
            String profileImage2 = user.getProfileImage();
            String birth = user.getBirth();
            user.setRefreshToken(jwtRefreshToken);
            userRepository.save(user);
            if(birth == null) {
                //JoinDetails가 없는 경우
                headers.add("JoinDetails", "false");
            } else {
                //JoinDetails가 있는 경우
                headers.add("JoinDetails", "true");
                // URL 인코딩하여 닉네임을 헤더에 추가
                String encodedNickname = URLEncoder.encode(nickname, StandardCharsets.UTF_8);
                headers.add("nickname", encodedNickname);
            }
            headers.add("partnership", String.valueOf(partner));
            headers.add("profileImage", profileImage2);
            headers.add("email", email);
        }else {
            return ResponseEntity.badRequest().body("error");
        }
        headers.add("Authorization", "Bearer " + jwtAccessToken);
        headers.add("refresh_token", jwtRefreshToken);

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().headers(headers).body("카카오 로그인 성공");
    }
    //https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=53035b0763b5b30c2d9270a501fb614b&redirect_uri=https://port-0-swr-17xco2nlst8pr67.sel5.cloudtype.app/callback
}
