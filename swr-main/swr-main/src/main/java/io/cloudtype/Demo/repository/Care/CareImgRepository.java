package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CareImgEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareImgRepository extends JpaRepository<CareImgEntity, Long> {
    List<CareImgEntity>  findByCarePost_Id(int carePostId);
}
