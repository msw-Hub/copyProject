package io.cloudtype.Demo.mypage.pet;

import io.cloudtype.Demo.mypage.user.UserEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<PetEntity, Long> {
    PetEntity findById(int petId);

    @Query("SELECT p.petImage FROM PetEntity p WHERE p.id = :petId")
    String findImageUrlById(int petId);

    List<PetEntity> findByOwner(UserEntity owner);
    void delete(@NotNull PetEntity petEntity);


}
