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
public class QuestionThreadEntity extends ThreadEntity{
    public QuestionThreadEntity(ForumEntity forum, UUID creatorId, String title, @NotNull PostEntity question) {
        super(forum, creatorId, title);
        this.question = question;
    }

    @Builder
    public QuestionThreadEntity(UUID id, ForumEntity forum, UUID creatorId, String title, OffsetDateTime creationTime,
                                List<PostEntity> posts, Integer numberOfPosts, ThreadMediaRecordReferenceEntity threadMediaRecordReference,
                                @NotNull PostEntity question, PostEntity selectedAnswer) {
        super(id, forum, creatorId, title, creationTime, posts, numberOfPosts, threadMediaRecordReference);
        this.question = question;
        this.selectedAnswer = selectedAnswer;
    }

    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @NotNull
    PostEntity question;

    @OneToOne
    PostEntity selectedAnswer;
}
