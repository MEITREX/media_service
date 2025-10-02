package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "ExerciseSolution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSolutionEntity implements IWithId<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private OffsetDateTime submissionDate;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileEntity> files;

    @OneToOne(cascade = CascadeType.ALL)
    private ResultEntity result;
}
