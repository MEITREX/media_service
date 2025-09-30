package de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity(name = "Post")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @CreationTimestamp
    OffsetDateTime creationTime;

    @Column(nullable = false)
    UUID authorId;

    @Column
    boolean edited;

    @Column
    UUID referenceId;

    @ElementCollection
    private List<UUID> downvotedByUsers;

    @ElementCollection
    private List<UUID> upvotedByUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    ThreadEntity thread;

    public PostEntity(String content, UUID authorId, ThreadEntity thread) {
        this.content = content;
        this.authorId = authorId;
        this.thread = thread;
        referenceId = null;
        downvotedByUsers = new ArrayList<>();
        upvotedByUsers = new ArrayList<>();
        edited = false;
    }

    public PostEntity(String content, UUID authorId) {
        this.content = content;
        this.authorId = authorId;
        downvotedByUsers = new ArrayList<>();
        upvotedByUsers = new ArrayList<>();
        edited = false;
    }

    @Override
    public String toString() {
        if (thread == null) {
            return "id: " +  id + ", content: " + content + ", authorId: " + authorId + ", thread: null"  +
                    ", creationTime: " + creationTime + ", downvotedByUsers: " + downvotedByUsers +
                    ", upvotedByUsers " + upvotedByUsers;
        }
        return "id: " +  id + ", content: " + content + ", authorId: " + authorId + ", threadId: " + thread.getId() +
                ", creationTime: " + creationTime + ", downvotedByUsers: " + downvotedByUsers +
                ", upvotedByUsers " + upvotedByUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostEntity that = (PostEntity) o;
        if (this.thread == null && that.thread == null) return true;
        if (this.thread == null) return false;
        return Objects.equals(id, that.id) && Objects.equals(content, that.content) && Objects.equals(creationTime, that.creationTime) && Objects.equals(authorId, that.authorId) && Objects.equals(downvotedByUsers, that.downvotedByUsers) && Objects.equals(upvotedByUsers, that.upvotedByUsers) && Objects.equals(thread.getId(), that.thread.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, creationTime, authorId, downvotedByUsers, upvotedByUsers, thread.getId());
    }
}
