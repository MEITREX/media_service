package de.unistuttgart.iste.gits.media_service.persistence.repository;

import de.unistuttgart.iste.gits.media_service.dapr.dao.MediaRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRecordRepository extends JpaRepository<MediaRecordEntity, UUID> {

    Optional<MediaRecordEntity> findByName(String name);

}
