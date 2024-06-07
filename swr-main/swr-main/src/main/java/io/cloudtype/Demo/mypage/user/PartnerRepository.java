package io.cloudtype.Demo.mypage.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartnerRepository extends JpaRepository<PartnerEntity, Long> {
    PartnerEntity findByUser_Id(int id);
}

