package io.cloudtype.Demo.repository;

import io.cloudtype.Demo.entity.PetEntity;
import io.cloudtype.Demo.entity.UserEntity;
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
