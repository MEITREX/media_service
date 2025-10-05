package de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

@Entity(name = "ThreadContentReference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThreadContentReferenceEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    UUID contentId;

    @OneToOne
    ThreadEntity thread;

    @Column
    private Integer timeStampSeconds;

    @Column
    private Integer pageNumber;

    public ThreadContentReferenceEntity(ThreadEntity thread, UUID contentId, Integer timeStamp, Integer pageNumber) {
        this.contentId = contentId;
        this.thread = thread;
        this.timeStampSeconds = timeStamp;
        this.pageNumber = pageNumber;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{ ", " }");
        if (contentId != null) {
            joiner.add("contentId: " + contentId);
        }
        if (thread != null) {
            joiner.add("threadId: " + thread.getId());
        }
        if (timeStampSeconds != null) {
            joiner.add("timeStampSeconds: " + timeStampSeconds);
        }
        if (pageNumber != null) {
            joiner.add("pageNumber: " + pageNumber);
        }
        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadContentReferenceEntity that = (ThreadContentReferenceEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(contentId, that.contentId) && Objects.equals(thread.getId(), that.thread.getId()) && Objects.equals(timeStampSeconds, that.timeStampSeconds) && Objects.equals(pageNumber, that.pageNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contentId, thread.getId(), timeStampSeconds, pageNumber);
    }
}
