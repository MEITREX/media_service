package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ForumEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ForumRepository extends JpaRepository<ForumEntity, UUID> {
    Optional<ForumEntity> findByCourseId(UUID courseId);

    List<ForumEntity> findAllByUserIdsContaining(UUID userId);
}
