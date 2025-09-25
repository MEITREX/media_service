package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.FileEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubmissionFileRepository extends MeitrexRepository<FileEntity, UUID> {
    @Query("select f from File f where f.uploadUrlExpiresAt is not null or f.downloadUrl is null")
    List<FileEntity> findCandidates();
}
