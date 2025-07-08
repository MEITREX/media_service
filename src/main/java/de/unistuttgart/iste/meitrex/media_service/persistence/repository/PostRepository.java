package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {
}
