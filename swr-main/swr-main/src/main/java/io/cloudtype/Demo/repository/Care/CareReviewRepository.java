package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareReviewRepository extends JpaRepository<CareReviewEntity, Long> {
    List<CareReviewEntity> findByCareRecode_Owner_IdOrderByCreateDateDesc(int userId);
    CareReviewEntity findById(int careReviewId);
    List<CareReviewEntity> findByCareRecode_Caregiver_IdOrderByCreateDateDesc(int userId);
}
