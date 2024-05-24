package io.cloudtype.Demo.controller;

import io.cloudtype.Demo.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @PostMapping("/tossPay")
    public ResponseEntity<?> tossPay(@RequestHeader("Authorization") String accessToken,
                                     @RequestBody Map<String, Object> requestBody
    ) {
        try {
            String orderId = (String) requestBody.get("orderId");
            String amount = (String) requestBody.get("amount");
            String paymentKey = (String) requestBody.get("paymentKey");
            paymentService.tossPay(accessToken, orderId, amount, paymentKey);
            return ResponseEntity.ok("결제 성공");
            } catch (IllegalStateException | IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
