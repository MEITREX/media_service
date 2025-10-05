package de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "Thread")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class ThreadEntity implements Serializable {

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostEntity> posts;

    @Column(nullable = false)
    Integer numberOfPosts;

    @OneToOne(cascade = CascadeType.ALL)
    ThreadContentReferenceEntity threadContentReference;

    protected ThreadEntity(ForumEntity forum, UUID creatorId, String title) {
        this.forum = forum;
        this.creatorId = creatorId;
        this.title = title;
        this.posts = new ArrayList<>();
        numberOfPosts = 0;
    }

    @Override
    public String toString() {
        return "ThreadEntity{" +
                "id=" + id +
                ", forumId=" + forum.getId() +
                ", creatorId=" + creatorId +
                ", title='" + title + '\'' +
                ", creationTime=" + creationTime +
                ", posts=" + posts +
                ", numberOfPosts=" + numberOfPosts +
                ", threadContentReference=" + threadContentReference +
                '}';
    }
}
