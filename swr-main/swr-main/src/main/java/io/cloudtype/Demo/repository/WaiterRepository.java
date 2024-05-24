package io.cloudtype.Demo.repository;

import io.cloudtype.Demo.entity.WaiterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaiterRepository extends JpaRepository<WaiterEntity, Long> {
    WaiterEntity findByWalkMatchingIdAndWaiterId(int walkMatchingId, int waiterId);

    List<WaiterEntity> findByWaiterId(int waiterId);
    List<WaiterEntity> findByWalkMatchingId(int walkMatchingId);
    WaiterEntity findById(int id);
    void deleteByWalkMatchingId(int walkMatchingId);
    void deleteByWaiterId(int waiterId);
}
