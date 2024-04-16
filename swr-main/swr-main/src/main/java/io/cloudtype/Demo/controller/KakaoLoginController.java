package io.cloudtype.Demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.service.KakaoService;
import io.cloudtype.Demo.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
    private UserInfoService userInfoService; // UserInfoService 주입

    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 위한 API")
    @Parameter(name = "code", description = "카카오 로그인 시 발급받은 코드", required = true)
    @ApiResponse(responseCode = "200", description = "로그인성공_이름,닉네임,토큰반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam("code") String code) throws IOException {
        Map<String, String> tokens = kakaoService.getTokensFromKakao(client_id, code);
        String accessToken = tokens.get("access_token");
        String expiresInStr = tokens.get("expires_in");
        int expiresIn = Integer.parseInt(expiresInStr);
        String refreshToken = tokens.get("refresh_token");
        String refreshTokenExpiresInStr = tokens.get("refresh_token_expires_in");
        int refreshTokenExpiresIn = Integer.parseInt(refreshTokenExpiresInStr);

        log.info("Access Token : " + accessToken);

        // 현재 시간을 가져옴
        Instant now = Instant.now();

        // 만료 시간을 현재 시간에 만료 기간을 더한 값으로 계산
        Instant expiresAtInstant = now.plusSeconds(expiresIn);
        long expiresAtUnix = expiresAtInstant.getEpochSecond();

        // refreshToken 만료 시간을 계산
        Instant refreshTokenExpiresAtInstant = now.plusSeconds(refreshTokenExpiresIn);
        long refreshTokenExpiresAtUnix = refreshTokenExpiresAtInstant.getEpochSecond();

        log.info(String.valueOf(expiresAtUnix));

        Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
        String nickName = (String) userInfo.get("nickname");
        String email = (String) userInfo.get("email");

        //데이터베이스에 있는 내용인지 검토
        int count = kakaoService.processUser(userInfo);

        String logMessage = count == 0 ? "회원가입" : "로그인";
        log.info(logMessage);

        // JSON 응답에 포함할 데이터 준비
        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("login_or_sign", logMessage);
        jsonResponse.put("access_token", accessToken);
        jsonResponse.put("access_token_expires_in", expiresAtUnix);
        jsonResponse.put("refresh_token", refreshToken);
        jsonResponse.put("refresh_token_expires_in", refreshTokenExpiresAtUnix);

        // ObjectMapper를 사용하여 Map 객체를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(jsonResponse);

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().body(jsonString);
    }


    @Operation(summary = "엑세스토큰갱신" , description = "엑세스토큰 갱신을 위한 API")
    @Parameter(name = "refresh_token", description = "리프레시 토큰", required = true)
    @ApiResponse(responseCode = "200", description = "엑세스토큰갱신성공_토큰반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestBody Map<String, String> requestBody) throws IOException {
        String refreshedToken = requestBody.get("refresh_token");
        log.info("Received refresh token: " + refreshedToken);

        // KakaoService를 통해 액세스 토큰 갱신 요청
        Map<String, String> tokens = kakaoService.refreshAccessToken(client_id, refreshedToken);
        String accessToken = tokens.get("access_token");
        String expiresInStr = tokens.get("expires_in");
        int expiresIn = Integer.parseInt(expiresInStr);
        String refreshToken = tokens.get("refresh_token");

        // 현재 시간을 가져옴
        Instant now = Instant.now();
        // 만료 시간을 현재 시간에 만료 기간을 더한 값으로 계산
        Instant expiresAtInstant = now.plusSeconds(expiresIn);
        long expiresAtUnix = expiresAtInstant.getEpochSecond();

        Map<String, Object> jsonResponse = new HashMap<>();
        jsonResponse.put("access_token", accessToken);
        jsonResponse.put("access_token_expires_in", expiresAtUnix);

        if (refreshToken!=null){
            String refreshTokenExpiresInStr = tokens.get("refresh_token_expires_in");
            int refreshTokenExpiresIn = Integer.parseInt(refreshTokenExpiresInStr);

            // refreshToken 만료 시간을 계산
            Instant refreshTokenExpiresAtInstant = now.plusSeconds(refreshTokenExpiresIn);
            long refreshTokenExpiresAtUnix = refreshTokenExpiresAtInstant.getEpochSecond();

            jsonResponse.put("refresh_token", refreshToken);
            jsonResponse.put("refresh_token_expires_in", refreshTokenExpiresAtUnix);

        };

        // ObjectMapper를 사용하여 Map 객체를 JSON 문자열로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonString = objectMapper.writeValueAsString(jsonResponse);

        // 프론트엔드에 전달할 응답 생성
        return ResponseEntity.ok().body(jsonString);
    }


    @Operation(summary = "닉네임중복검사", description = "회원가입을 위한 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "nickname", description = "닉네임", required = true)
    @ApiResponse(responseCode = "200", description = "데이터저장성공_성공메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/signup/nickname")
    public ResponseEntity<String> signUpNickname(@RequestHeader("Authorization") String accessToken,
                                         @RequestBody Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);
            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");
            // 회원가입 시 추가 정보를 데이터베이스에 저장
            String nickName = requestBody.get("nickname");
            // 공백 문자가 있는지 확인하여 처리
            if (nickName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("입력값에 공백 문자가 포함되어 있습니다. 다시 입력해주세요.");
            }

            // 닉네임 중복 확인
            List<String> allNicknames = userInfoService.getAllNicknames();
            boolean isNicknameDuplicate = allNicknames.stream()
                    .anyMatch(nickname -> nickname.equalsIgnoreCase(nickName));
            if (isNicknameDuplicate) {
                return ResponseEntity.badRequest().body("이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해주세요.");
            }
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", "닉네임 중복 없음_성공메세지반환");
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(jsonResponse);
            return ResponseEntity.ok().body(jsonString);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @Operation(summary = "회원가입추가정보", description = "회원가입을 위한 API")
    @Parameter(name = "Authorization", description = "Access Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "nickname", description = "닉네임", required = true)
    @Parameter(name = "phone_number", description = "전화번호", required = true)
    @Parameter(name = "pin_number", description = "핀번호", required = true)
    @Parameter(name = "birthday", description = "생년월일", required = true)
    @ApiResponse(responseCode = "200", description = "데이터저장성공_성공메세지반환", content = @Content(mediaType = "application/json",schema = @Schema(implementation = String.class)))
    @CrossOrigin(origins = {"https://teamswr.store", "http://localhost:5173"})
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestHeader("Authorization") String accessToken,
                                         @RequestBody Map<String, String> requestBody) {
        try {
            // 카카오 서버에서 해당 엑세스 토큰을 사용하여 유저 정보를 가져옴
            Map<String, Object> userInfo = kakaoService.getUserInfo(accessToken);

            // 가져온 유저 정보에서 고유 ID를 확인
            Long userId = (Long) userInfo.get("userId");

            // 회원가입 시 추가 정보를 데이터베이스에 저장
            String nickName = requestBody.get("nickname");
            String phoneNumber = requestBody.get("phone_number");
            String pinNumber = requestBody.get("pin_number");
            String birthday = requestBody.get("birthday");

            // 공백 문자가 있는지 확인하여 처리
            if (nickName.trim().isEmpty() || phoneNumber.trim().isEmpty() || pinNumber.trim().isEmpty() || birthday.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("입력값에 공백 문자가 포함되어 있습니다. 다시 입력해주세요.");
            }

            userInfoService.saveAdditionalUserInfo(userId, nickName, phoneNumber, pinNumber, birthday);

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", "4개의 데이터 문제없이 받아서 저장함");
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(jsonResponse);
            return ResponseEntity.ok().body(jsonString);
        } catch (IOException e) {
            log.error("Failed to fetch user info from Kakao API", e);
            return ResponseEntity.internalServerError().build();
        }
    }

}
