package io.cloudtype.Demo.mail;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Setter
@Getter
public class EmailCheckEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String email;
    private String authNum;
    private LocalDateTime tryDateTime;

    @PrePersist
    public void prePersist() {
        this.tryDateTime = LocalDateTime.now();
    }
}
