package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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
    private UUID userId;

    @Column
    private Status status;

    @OneToMany(cascade = CascadeType.ALL)
    private List<TaskResultEntity> results;
}
