package io.cloudtype.Demo.care.repository;

import io.cloudtype.Demo.care.entity.CarePostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarePostRepository extends JpaRepository<CarePostEntity, Long> {
    CarePostEntity findByCaregiver_Id(int caregiverId);
    CarePostEntity findById(int id);
    List<CarePostEntity> findByAdministrativeAddress1AndAdministrativeAddress2(String administrativeAddress1, String administrativeAddress2);
}
