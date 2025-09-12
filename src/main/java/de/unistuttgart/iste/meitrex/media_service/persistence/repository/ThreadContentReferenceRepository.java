package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ThreadContentReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ThreadContentReferenceRepository extends JpaRepository<ThreadContentReferenceEntity, UUID> {
    List<ThreadContentReferenceEntity> findAllByContentId(UUID contentId);
}
