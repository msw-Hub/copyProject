package io.cloudtype.Demo.repository.Care;

import io.cloudtype.Demo.entity.Care.CarePostEntity;
import io.cloudtype.Demo.entity.Care.ReservationSchedulerEntity;
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
