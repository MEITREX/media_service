package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubmissionExerciseRepository extends MeitrexRepository<SubmissionExerciseEntity, UUID> {
}
