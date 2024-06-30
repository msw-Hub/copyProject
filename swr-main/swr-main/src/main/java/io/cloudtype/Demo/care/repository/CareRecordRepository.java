package io.cloudtype.Demo.care.repository;

import io.cloudtype.Demo.care.entity.CareRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareRecordRepository extends JpaRepository<CareRecordEntity, Long> {
    CareRecordEntity findById(int careRecodeId);
    List<CareRecordEntity> findByOwner_IdOrderByEndDateDesc(int userId);
    List<CareRecordEntity> findByCaregiver_IdOrderByEndDateDesc(int caregiverId);
}
