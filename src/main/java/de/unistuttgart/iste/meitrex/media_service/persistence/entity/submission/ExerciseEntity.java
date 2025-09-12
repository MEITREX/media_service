package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "Exercise")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private UUID submissionId;

    private OffsetDateTime startTime;

    private OffsetDateTime endTime;

    private int maxScore;

    @OneToMany(cascade = CascadeType.ALL)
    private List<FileEntity> files;

    @OneToMany
    private List<ExerciseSolutionEntity> solutions;

    @OneToMany
    private List<TaskEntity> tasks;
}
