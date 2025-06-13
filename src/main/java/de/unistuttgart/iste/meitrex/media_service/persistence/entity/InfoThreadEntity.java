package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@Data
public class InfoThreadEntity extends ThreadEntity{
    public InfoThreadEntity(ForumEntity forum, UUID creatorId, String title, @NotNull PostEntity info) {
        super(forum, creatorId, title);
        this.info = info;
    }

    @Builder
    public InfoThreadEntity(UUID id, ForumEntity forum, UUID creatorId, String title, OffsetDateTime creationTime, List<PostEntity> posts, Integer numberOfPosts, ThreadMediaRecordReferenceEntity threadMediaRecordReference, @NotNull PostEntity info) {
        super(id, forum, creatorId, title, creationTime, posts, numberOfPosts, threadMediaRecordReference);
        this.info = info;
    }


    @OneToOne
    @NotNull
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    PostEntity info;
}
