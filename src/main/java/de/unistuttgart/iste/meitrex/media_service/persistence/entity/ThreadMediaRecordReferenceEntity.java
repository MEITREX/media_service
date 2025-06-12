package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import de.unistuttgart.iste.meitrex.media_service.persistence.embeddable.ThreadMediaRecordReferenceKey;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;
import org.hibernate.annotations.Cascade;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Entity(name = "ThreadMediaRecordReference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThreadMediaRecordReferenceEntity implements Serializable {
    @EmbeddedId
    ThreadMediaRecordReferenceKey id = new ThreadMediaRecordReferenceKey();

    @ManyToOne
    @MapsId("mediaRecordId")
    @JoinColumn(name = "media_record_id")
    MediaRecordEntity mediaRecord;

    @OneToOne
    @MapsId("threadId")
    @JoinColumn(name = "thread_id")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    ThreadEntity thread;

    @Column
    private Integer timeStampSeconds;

    @Column
    private Integer pageNumber;

    public ThreadMediaRecordReferenceEntity(ThreadEntity thread, MediaRecordEntity mediaRecord, Integer timeStamp, Integer pageNumber) {
        id.setMediaRecordId(mediaRecord.getId());
        id.setThreadId(thread.getId());
        this.thread = thread;
        this.mediaRecord = mediaRecord;
        this.timeStampSeconds = timeStamp;
        this.pageNumber = pageNumber;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{ ", " }");
        if (mediaRecord != null) {
            joiner.add("mediaRecordId: " + mediaRecord.getId());
        }
        if (thread != null) {
            joiner.add("threadId: " + thread.getId());
        }
        joiner.add("timeStampSeconds: " + timeStampSeconds);
        joiner.add("pageNumber: " + pageNumber);

        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadMediaRecordReferenceEntity that = (ThreadMediaRecordReferenceEntity) o;
        return Objects.equals(id, that.id) && Objects.equals(mediaRecord, that.mediaRecord) && Objects.equals(thread.getId(), that.thread.getId()) && Objects.equals(timeStampSeconds, that.timeStampSeconds) && Objects.equals(pageNumber, that.pageNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mediaRecord, thread.getId(), timeStampSeconds, pageNumber);
    }
}
