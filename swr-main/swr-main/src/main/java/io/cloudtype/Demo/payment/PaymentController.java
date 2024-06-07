package io.cloudtype.Demo.payment;

import io.cloudtype.Demo.payment.DTO.PaymentDTO;
import io.cloudtype.Demo.payment.DTO.PaymentListDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/getUserInfo")
    public ResponseEntity<?> page(@RequestHeader("Authorization") String accessToken
    ){
        try {
            Map<String,String> userInfo = paymentService.getInfo(accessToken);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    //결제 요청전에 결제할 데이터 저장(검증준비)
    @PostMapping("/beforePayment")
    public ResponseEntity<?> beforePayment(@RequestHeader("Authorization") String accessToken,
                                           @RequestBody PaymentDTO paymentDTO
    ){
        try {
            String orderId = paymentDTO.getOrderId();
            int amount = paymentDTO.getAmount();
            paymentService.beforePayment(accessToken, orderId, amount);
            return ResponseEntity.ok("결제 준비 완료");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    //클라이언트가 토스페이를 통해 결제를 진행한 후 서버에서의 최종 결제 승인
    @PostMapping("/tossPay")
    public ResponseEntity<?> tossPay(@RequestHeader("Authorization") String accessToken,
                                     @RequestBody PaymentDTO paymentDTO
    ){
        try {
            String orderId = paymentDTO.getOrderId();
            int amount = paymentDTO.getAmount();
            String paymentKey = paymentDTO.getPaymentKey();
            log.info("paymentKey : " + paymentKey);
            log.info("orderId : " + orderId);
            log.info("amount : " + amount);

            //결제전 order_id로 payment_id를 가져와서 검증
            paymentService.checkPayment(accessToken, orderId, amount, paymentKey);
            //결제 승인
            paymentService.tossPay(accessToken, orderId, amount, paymentKey);
            return ResponseEntity.ok("결제 성공");
            } catch (IllegalStateException | IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // 본인 결제 내역 조회
    @GetMapping("/paymentList")
    public ResponseEntity<?> paymentList(@RequestHeader("Authorization") String accessToken){
        try {
            List<PaymentListDTO> paymentList = paymentService.paymentList(accessToken);
            return ResponseEntity.ok(paymentList);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
