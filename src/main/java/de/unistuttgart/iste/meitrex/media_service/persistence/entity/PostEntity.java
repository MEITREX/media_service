package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity(name = "Post")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostEntity {
    public PostEntity(String title, String content, UUID authorId, ThreadEntity thread, QuestionThreadEntity question, QuestionThreadEntity selectedAnswer) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.thread = thread;
        this.question = question;
        this.selectedAnswer = selectedAnswer;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String content;

    @CreationTimestamp
    LocalDateTime creationTime;

    @Column(nullable = false)
    UUID authorId;

    @ElementCollection
    Set<UUID> downvotedByUsers;

    @ElementCollection
    Set<UUID> upvotedByUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    ThreadEntity thread;

    @OneToOne
    QuestionThreadEntity question;

    @OneToOne
    QuestionThreadEntity selectedAnswer;
}
