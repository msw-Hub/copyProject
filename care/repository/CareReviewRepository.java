package io.cloudtype.Demo.care.repository;

import io.cloudtype.Demo.care.entity.CareReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareReviewRepository extends JpaRepository<CareReviewEntity, Long> {
    List<CareReviewEntity> findByCareRecord_Owner_IdOrderByCreateDateDesc(int userId);
    CareReviewEntity findById(int careReviewId);
    List<CareReviewEntity> findByCareRecord_Caregiver_IdOrderByCreateDateDesc(int userId);
}
