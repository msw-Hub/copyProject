package io.cloudtype.Demo.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    PaymentEntity findByOrderId(String orderId);
    List<PaymentEntity> findByConsumer_IdOrderByOrderAtDesc(int userId);
}
