package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadMediaRecordReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreadMediaRecordReferenceRepository extends JpaRepository<ThreadMediaRecordReferenceEntity, UUID> {
    List<ThreadMediaRecordReferenceEntity> findAllByMediaRecord(MediaRecordEntity mediaRecord);
}
