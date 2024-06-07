package io.cloudtype.Demo.payment.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class PaymentListDTO {
    private String orderID; // 주문번호
    private int amount; // 결제금액
    private String paymentType; // 결제수단
    private String orderName; // 주문명
    private LocalDateTime orderAt; // 주문시간
    private String status; // 결제상태
}
