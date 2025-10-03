package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordProgressDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaRecordProgressDataRepository
        extends JpaRepository<MediaRecordProgressDataEntity, MediaRecordProgressDataEntity.PrimaryKey> {

}
