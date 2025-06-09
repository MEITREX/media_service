package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.embeddable.ThreadMediaRecordReferenceKey;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadMediaRecordReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ThreadMediaRecordReferenceRepository extends JpaRepository<ThreadMediaRecordReferenceEntity, ThreadMediaRecordReferenceKey> {
}
