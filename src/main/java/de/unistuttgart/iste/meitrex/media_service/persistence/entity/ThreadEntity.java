package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ThreadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    ForumEntity forum;

    @Column(nullable = false)
    private UUID creatorId;

    @Column(nullable = false)
    String title;

    @CreationTimestamp
    LocalDateTime creationTime;
}
