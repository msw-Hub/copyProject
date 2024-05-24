package io.cloudtype.Demo.service;

import io.cloudtype.Demo.entity.UserEntity;
import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class PaymentService {
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${kakao.cid}")
    private String KakaoCid;

    @Value("${kakao.SECRET_KEY}")
    private String kakaoSecretKey;

    @Value("${toss.secret_key}")
    private String tossSecretKey;

    @Autowired
    public PaymentService(JWTUtil jwtUtil, UserRepository userRepository,
                          @Value("${toss.secret_key}") String tossSecretKey,
                          @Value("${kakao.SECRET_KEY}") String kakaoSecretKey,
                            @Value("${kakao.cid}") String KakaoCid
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tossSecretKey = tossSecretKey;
        this.kakaoSecretKey = kakaoSecretKey;
        this.KakaoCid = KakaoCid;
    }
    public void tossPay(String accessToken, String orderId, String amount, String paymentKey) throws IOException {
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }

        // toss POST 요청 URL
        String reqURL = "https://api.tosspayments.com/v1/payments/confirm";
        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Basic " + tossSecretKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        // JSON 데이터 생성
        String jsonInputString = "{"
                + "\"paymentKey\":\"" + paymentKey + "\","
                + "\"orderId\":\"" + orderId + "\","
                + "\"amount\":\"" + amount + "\""
                + "}";
        // 데이터 출력 스트림에 JSON 데이터 작성
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(jsonInputString);
            wr.flush();
        }

        // 응답 읽기
        int responseCode = conn.getResponseCode();
        InputStreamReader streamReader;

        // 응답 코드에 따른 스트림 설정
        if (responseCode >= 200 && responseCode < 300) {
            streamReader = new InputStreamReader(conn.getInputStream());
        } else {
            streamReader = new InputStreamReader(conn.getErrorStream());
        }

        BufferedReader br = new BufferedReader(streamReader);
        String line;
        StringBuilder result = new StringBuilder();

        while ((line = br.readLine()) != null) {
            result.append(line);
        }
        br.close();

        // 결과 출력 (디버그 용도)
        log.info("Response Code: {}", responseCode);
        log.info("Response: {}", result);
    }
}
