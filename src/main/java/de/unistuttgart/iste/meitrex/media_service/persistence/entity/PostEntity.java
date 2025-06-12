package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
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
public class PostEntity {
    public PostEntity(String content, UUID authorId, ThreadEntity thread) {
        this.content = content;
        this.authorId = authorId;
        this.thread = thread;
    }

    public PostEntity(String content, UUID authorId) {
        this.content = content;
        this.authorId = authorId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    String content;

    @CreationTimestamp
    OffsetDateTime creationTime;

    @Column(nullable = false)
    UUID authorId;

    @ElementCollection
    List<UUID> downvotedByUsers;

    @ElementCollection
    List<UUID> upvotedByUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    ThreadEntity thread;

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
        if (o == this) return true;
        if (!(o instanceof PostEntity other)) {
            return false;
        }

        boolean equalsId = (this.id == null && other.id == null) || (this.id != null && this.id.equals(other.id));
        boolean equalsContent = (this.content == null && other.content == null) ||
                (this.content != null && this.content.equals(other.content));
        boolean equalsCreationTime = (this.creationTime == null && other.creationTime == null) ||
                (this.creationTime != null && this.creationTime.equals(other.creationTime));
        boolean equalsAuthorId = (this.authorId == null && other.authorId == null) ||
                (this.authorId != null && this.authorId.equals(other.authorId));
        boolean equalsDownvotedByUsers = (this.downvotedByUsers == null && other.downvotedByUsers == null) ||
                (this.downvotedByUsers != null && this.downvotedByUsers.equals(other.downvotedByUsers));
        boolean equalsUpvodedByUsers = (this.upvotedByUsers == null && other.upvotedByUsers == null) ||
                (this.upvotedByUsers != null && this.upvotedByUsers.equals(other.upvotedByUsers));
        boolean equalsThread = (this.thread == null && other.thread == null) ||
                (this.thread != null && this.thread.getId() != null && this.thread.getId().equals(other.thread.getId()));
        return equalsId && equalsContent && equalsCreationTime && equalsAuthorId && equalsDownvotedByUsers &&
                equalsUpvodedByUsers && equalsThread;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, creationTime, authorId, downvotedByUsers, upvotedByUsers, thread.getId());
    }
}
