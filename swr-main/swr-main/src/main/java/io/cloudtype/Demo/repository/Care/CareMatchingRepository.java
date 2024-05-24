package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareMatchingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareMatchingRepository extends JpaRepository<CareMatchingEntity, Long> {
    boolean existsByCarePost_IdAndStatus(int carePostId, int status);
    List<CareMatchingEntity> findByCarePost_Caregiver_IdAndStatus(int caregiverId, int status);
    List<CareMatchingEntity> findByOwner_IdAndStatus(int userId, int status);
    CareMatchingEntity findById(int id);
}
