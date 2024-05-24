package io.cloudtype.Demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class KakaoService {


    // 생성자를 통한 주입
    private final UserRepository userRepository;

    public KakaoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    //로그인 첫 토큰 발행
    public Map<String, String> getTokensFromKakao(String client_id, String code) throws IOException {
        //------kakao POST 요청------
        String reqURL = "https://kauth.kakao.com/oauth/token?grant_type=authorization_code&client_id="+client_id+"&code=" + code;
        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line = "";
        String result = "";

        while ((line = br.readLine()) != null) {
            result += line;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> tokens = objectMapper.readValue(result, new TypeReference<Map<String, String>>() {});

        log.info("Response Body : " + result);

        return tokens;
    }

    //카카오로부터 유저정보 받아오는 메서드
    public Map<String, Object> getUserInfo(String access_Token) throws IOException {
        // 클라이언트 요청 정보
        Map<String, Object> userInfo = new HashMap<>();

        //------kakao GET 요청------
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + access_Token);

        int responseCode = conn.getResponseCode();
        System.out.println("responseCode : " + responseCode);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            result.append(line);
        }

        // jackson objectmapper 객체 생성
        ObjectMapper objectMapper = new ObjectMapper();

        // JSON String -> Map
        Map<String, Object> jsonMap = objectMapper.readValue(result.toString(), new TypeReference<Map<String, Object>>() {});

        // 프로필 정보 가져오기
        Map<String, Object> properties = (Map<String, Object>) jsonMap.get("properties");
        String profileImage = (String) properties.get("profile_image");

        // 카카오 계정 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) jsonMap.get("kakao_account");
        String email = (String) kakaoAccount.get("email");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String name = (String) profile.get("nickname");
        String gender = (String) kakaoAccount.get("gender");

        // userInfo에 넣기
        userInfo.put("profileImage", profileImage);
        userInfo.put("email", email);
        userInfo.put("name", name);
        userInfo.put("gender", gender);

        return userInfo;
    }

    //사용자 정보 처리 메서드 (DB에 이미 존재하는지 확인후 있으면 로그인, 없으면 회원가입 진행)
    public int processUser(String email) {
        return isUsernameTaken(email);
    }
    @Transactional(readOnly = true)
    public int isUsernameTaken(String username) {
        UserEntity user = userRepository.findByUsername(username);

        if (user == null) {
            return 0; // 사용 가능한 username
        } else {
            return user.isKakaoLogin() ? 1 : 2; // 카카오 로그인 여부에 따라 반환값 설정
        }
    }
    public void signUp(String email, String role, String profile, String name, String gender, String jwtRefreshToken) {
        UserEntity user = new UserEntity();
        user.setUsername(email);
        user.setRole(role);
        user.setProfileImage(profile);
        user.setName(name);
        user.setGender(gender);
        user.setRefreshToken(jwtRefreshToken);
        user.setKakaoLogin(true);
        user.setPartnership(0);

        userRepository.save(user);
    }
}