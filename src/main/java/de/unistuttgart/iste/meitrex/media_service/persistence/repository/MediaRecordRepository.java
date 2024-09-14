package de.unistuttgart.iste.meitrex.media_service.persistence.repository;

import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaRecordRepository extends MeitrexRepository<MediaRecordEntity, UUID>, JpaSpecificationExecutor<MediaRecordEntity> {

    // Query annotation is necessary to be able to match a list against another
    @Query("SELECT DISTINCT media FROM MediaRecord media JOIN media.contentIds actualIds WHERE actualIds IN :contentIds")
    List<MediaRecordEntity> findMediaRecordEntitiesByContentIds(@Param("contentIds") List<UUID> contentIds);

    List<MediaRecordEntity> findMediaRecordEntitiesByCreatorId(UUID creatorId);

    @Query("SELECT DISTINCT media FROM MediaRecord media JOIN media.courseIds actualIds WHERE actualIds IN :courseIds")
    List<MediaRecordEntity> findMediaRecordEntitiesByCourseIds(@Param("courseIds") List<UUID> courseIds);
}
