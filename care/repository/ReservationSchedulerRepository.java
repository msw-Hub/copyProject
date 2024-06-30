package io.cloudtype.Demo.care.repository;

import io.cloudtype.Demo.care.entity.CarePostEntity;
import io.cloudtype.Demo.care.entity.ReservationSchedulerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ReservationSchedulerRepository extends JpaRepository<ReservationSchedulerEntity, Long> {
    List<ReservationSchedulerEntity> findByUnavailableDateBetween(Date start, Date end);
    ReservationSchedulerEntity findByCarePostAndUnavailableDate(CarePostEntity carePost, Date unavailableDate);

    List<ReservationSchedulerEntity> findByCarePost(CarePostEntity carePost);
    boolean existsByCarePostAndReservationDate(CarePostEntity carePost, Date date);
    ReservationSchedulerEntity findByCarePostAndReservationDate(CarePostEntity carePost, Date date);
}
