package io.cloudtype.Demo.walk.repository;

import io.cloudtype.Demo.walk.entity.WalkRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkRecordRepository extends JpaRepository<WalkRecordEntity, Long> {
    WalkRecordEntity findById(int id);
    List<WalkRecordEntity> findByUser_IdOrderByCreateDateDesc(int userId);
    List<WalkRecordEntity> findByWalker_IdOrderByCreateDateDesc(int walkerId);
}
