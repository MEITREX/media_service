package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import jakarta.persistence.*;
import org.checkerframework.checker.units.qual.C;

import java.util.UUID;

@Entity(name = "Result")
public class ResultEntity {

    public enum Status {
        pending,
        passed,
        failed
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private Status status;

    @Column
    private int score;
}
