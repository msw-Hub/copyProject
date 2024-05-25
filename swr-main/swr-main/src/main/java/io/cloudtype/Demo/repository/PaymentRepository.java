package io.cloudtype.Demo.repository;

import io.cloudtype.Demo.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    PaymentEntity findByOrderId(String orderId);
    List<PaymentEntity> findByConsumer_IdOrderByOrderAtDesc(int userId);
}
