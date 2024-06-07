package io.cloudtype.Demo.care.repository;

import io.cloudtype.Demo.care.entity.CareImgEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareImgRepository extends JpaRepository<CareImgEntity, Long> {
    List<CareImgEntity>  findByCarePost_Id(int carePostId);
}
