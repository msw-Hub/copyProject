package io.cloudtype.Demo.repository.Walk;

import io.cloudtype.Demo.entity.Walk.WalkReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkReviewRepository extends JpaRepository<WalkReviewEntity, Long> {
    List<WalkReviewEntity> findByWalkRecode_User_IdOrderByCreateDateDesc(int userId);
    List<WalkReviewEntity> findByWalkRecode_Walker_IdOrderByCreateDateDesc(int walkerId);

    WalkReviewEntity findById(int id);
}
