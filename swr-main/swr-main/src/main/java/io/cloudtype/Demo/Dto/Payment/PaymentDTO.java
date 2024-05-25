package io.cloudtype.Demo.Dto.Payment;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PaymentDTO {
    private String paymentKey; // 결제키
    private String orderId; // 주문번호
    private int amount; // 결제금액
    private String paymentType; // 결제수단
}
