package io.cloudtype.Demo.walk.repository;

import io.cloudtype.Demo.walk.entity.WalkReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkReviewRepository extends JpaRepository<WalkReviewEntity, Long> {
    List<WalkReviewEntity> findByWalkRecord_User_IdOrderByCreateDateDesc(int userId);
    List<WalkReviewEntity> findByWalkRecord_Walker_IdOrderByCreateDateDesc(int walkerId);

    WalkReviewEntity findById(int id);
}
