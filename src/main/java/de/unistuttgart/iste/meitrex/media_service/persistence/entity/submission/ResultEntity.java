package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.C;

import java.util.UUID;

@Entity(name = "Result")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultEntity implements IWithId<UUID> {

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
