package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "Post")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostEntity {
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
    List<UUID> downvotedByUsers;

    @ElementCollection
    List<UUID> upvotedByUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    ThreadEntity thread;

    @OneToOne
    QuestionThreadEntity question;

    @OneToOne
    QuestionThreadEntity selectedAnswer;
}
