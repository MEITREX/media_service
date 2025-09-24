package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
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
public class SubmissionExerciseEntity implements IWithId<UUID> {
    @Id
    private UUID assessmentId;

    private UUID courseId;

    private OffsetDateTime endDate;

    @OneToMany(cascade = CascadeType.ALL)
    private List<FileEntity> files;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ExerciseSolutionEntity> solutions;

    @OneToMany(cascade = CascadeType.ALL)
    private List<TaskEntity> tasks;

    @Override
    public UUID getId() {
        return assessmentId;
    }
}
