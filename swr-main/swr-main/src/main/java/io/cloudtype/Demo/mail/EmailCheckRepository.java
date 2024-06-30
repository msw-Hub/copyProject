package io.cloudtype.Demo.mail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailCheckRepository extends JpaRepository<EmailCheckEntity, Long> {
    EmailCheckEntity findByEmail(String email);
}
