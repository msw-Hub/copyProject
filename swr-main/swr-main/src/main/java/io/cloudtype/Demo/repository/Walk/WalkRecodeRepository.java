package io.cloudtype.Demo.repository.Walk;

import io.cloudtype.Demo.entity.Walk.WalkRecodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkRecodeRepository extends JpaRepository<WalkRecodeEntity, Long> {
    WalkRecodeEntity findById(int id);
    List<WalkRecodeEntity> findByUser_IdOrderByCreateDateDesc(int userId);
    List<WalkRecodeEntity> findByWalker_IdOrderByCreateDateDesc(int walkerId);
}
