package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "ExerciseSolution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSolutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private OffsetDateTime submissionDate;

    @OneToMany(cascade = CascadeType.ALL)
    private List<FileEntity> files;

    @OneToOne
    private ResultEntity result;
}
