package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ThreadEntity {
    public ThreadEntity(ForumEntity forum, UUID creatorId, String title) {
        this.forum = forum;
        this.creatorId = creatorId;
        this.title = title;
        this.posts = new ArrayList<>();
        numberOfPosts = 0;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    ForumEntity forum;

    @Column(nullable = false)
    UUID creatorId;

    @Column(nullable = false)
    String title;

    @CreationTimestamp
    OffsetDateTime creationTime;

    @OneToMany
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    List<PostEntity> posts;

    @Column(nullable = false)
    Integer numberOfPosts;

    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    ThreadMediaRecordReferenceEntity threadMediaRecordReference;
}
