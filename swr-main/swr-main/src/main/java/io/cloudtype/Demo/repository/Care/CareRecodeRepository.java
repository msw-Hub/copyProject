package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareRecodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareRecodeRepository extends JpaRepository<CareRecodeEntity, Long> {
    CareRecodeEntity findById(int careRecodeId);
    List<CareRecodeEntity> findByOwner_IdOrderByEndDateDesc(int userId);
    List<CareRecodeEntity> findByCaregiver_IdOrderByEndDateDesc(int caregiverId);
}
