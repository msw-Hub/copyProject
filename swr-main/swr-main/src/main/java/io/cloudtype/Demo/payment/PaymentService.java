package io.cloudtype.Demo.payment;

import io.cloudtype.Demo.jwt.JWTUtil;
import io.cloudtype.Demo.mypage.user.UserEntity;
import io.cloudtype.Demo.mypage.user.UserRepository;
import io.cloudtype.Demo.payment.DTO.PaymentListDTO;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentService {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @Value("${toss.secret_key}")
    private final String tossSecretKey;

    @Autowired
    public PaymentService(JWTUtil jwtUtil, UserRepository userRepository,
                          @Value("${toss.secret_key}") String tossSecretKey,
                            PaymentRepository paymentRepository

    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tossSecretKey = tossSecretKey;
        this.paymentRepository = paymentRepository;
    }
    public void checkPayment(String accessToken, String orderId, int amount, String paymentKey) throws IOException {
        UserEntity user = getUserEntity(accessToken);
        //orderId로 db의 결제 정보 조회
        PaymentEntity paymentEntity = paymentRepository.findByOrderId(orderId);
        if (paymentEntity == null) {
            throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다");
        }
        //결제 정보 검증
        if (paymentEntity.getAmount() != amount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다");
        }
        if (paymentEntity.getConsumer().getId()!= user.getId()) {
            throw new IllegalArgumentException("결제 요청자가 일치하지 않습니다");
        }
        //프론트로 부터 받은 orderId
        log.info("orderId: {}", orderId);

        //paymentKey로 토스에게 결제 정보 조회한 payment객체와 orderId 일치하는지 확인
        //toss GET 요청 URL
        String reqURL = "https://api.tosspayments.com/v1/payments/" + paymentKey;
        log.info("reqURL: {}", reqURL);

        URL url = new URL(reqURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Basic " + tossSecretKey);
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

        // JSON 응답을 JSONObject로 파싱
        JSONObject jsonResponse = new JSONObject(result.toString());
        // paymentKey 추출
        String tossOrderId = jsonResponse.getString("orderId");
        String tossStatus = jsonResponse.getString("status");
        log.info("status: {}", tossStatus);
        log.info("orderId: {}", tossOrderId);
        // 결제 키 검증
        if(!tossStatus.equals("IN_PROGRESS")){
            throw new IllegalArgumentException("결제 진행중이 아닙니다");
        }
        if (!orderId.equals(tossOrderId)) {
            throw new IllegalArgumentException("결제 정보가 일치하지 않습니다");
        }
    }
    @Transactional
    public void tossPay(String accessToken, String orderId, int amount, String paymentKey) throws IOException {
        UserEntity user = getUserEntity(accessToken);

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
            //정상승인
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
        log.info("Response Code2: {}", responseCode);
        log.info("Response2: {}", result);


        // JSON 응답을 JSONObject로 파싱
        JSONObject jsonResponse = new JSONObject(result.toString());
        PaymentEntity paymentEntity = paymentRepository.findByOrderId(orderId);

        if(responseCode >= 200 && responseCode < 300) {
            //결제 성공
            String tossPaymentKey = jsonResponse.getString("paymentKey");
            String tossStatus = jsonResponse.getString("status");
            String tossOrderName = jsonResponse.getString("orderName");
            String tossPaymentType = jsonResponse.getString("method");
            //db에 저장
            paymentEntity.setPaymentKey(tossPaymentKey);
            paymentEntity.setStatus(tossStatus);
            paymentEntity.setOrderName(tossOrderName);
            paymentEntity.setPaymentType(tossPaymentType);
            paymentEntity.setApprovedAt(LocalDateTime.now());
            UserEntity consumer = paymentEntity.getConsumer();
            consumer.setCoin(consumer.getCoin() + amount); //코인 충전
            paymentRepository.save(paymentEntity);
        }else {
            //결제 실패
            String tossMessage = jsonResponse.getString("message");
            String tossStatus = jsonResponse.getString("status");
            paymentEntity.setStatus(tossStatus);
            paymentEntity.setFailReason(tossMessage);
            paymentRepository.save(paymentEntity);
            //실패사유 프론트로 전달
            throw new IllegalArgumentException("결제 실패: " + tossMessage);
        }
    }
    @Transactional
    public void beforePayment(String accessToken, String orderId, int amount) throws IOException {
        UserEntity user = getUserEntity(accessToken);
        //데이터베이스에 결제 요청 정보 저장
        PaymentEntity paymentEntity = new PaymentEntity();
        paymentEntity.setAmount(amount);
        paymentEntity.setOrderId(orderId);
        paymentEntity.setConsumer(user);
        paymentEntity.setOrderAt(LocalDateTime.now());
        paymentEntity.setStatus("READY");
        //결제 정보 저장
        paymentRepository.save(paymentEntity);
    }
    public List<PaymentListDTO> paymentList(String accessToken){
        UserEntity user = getUserEntity(accessToken);
        List<PaymentEntity> paymentList = paymentRepository.findByConsumer_IdOrderByOrderAtDesc(user.getId());
        return paymentList.stream()
                .map(this::createPaymentListDTO)
                .collect(Collectors.toList());
    }
    private PaymentListDTO createPaymentListDTO(PaymentEntity paymentEntity) {
        PaymentListDTO paymentListDTO = new PaymentListDTO();
        paymentListDTO.setOrderID(paymentEntity.getOrderId());
        paymentListDTO.setAmount(paymentEntity.getAmount());
        paymentListDTO.setPaymentType(paymentEntity.getPaymentType());
        paymentListDTO.setOrderName(paymentEntity.getOrderName());
        paymentListDTO.setOrderAt(paymentEntity.getOrderAt());
        paymentListDTO.setStatus(paymentEntity.getStatus());
        return paymentListDTO;
    }
    public Map<String,String> getInfo(String accessToken){
        UserEntity user = getUserEntity(accessToken);
        if(user.getPhoneNumber() == null){
            throw new IllegalArgumentException("전화번호를 등록해주세요");
        }
        Map<String,String> userInfo = new HashMap<>();
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("name", user.getName());
        return userInfo;
    }
    private UserEntity getUserEntity(String accessToken){
        accessToken = accessToken.split(" ")[1];
        String username = jwtUtil.getUsername(accessToken, 1);
        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다");
        }
        return user;
    }
}
