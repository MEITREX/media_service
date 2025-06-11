package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;

import java.io.Serializable;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

@Entity(name = "ThreadMediaRecordReference")
@Getter
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThreadMediaRecordReferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    UUID id;

    @ManyToOne
    MediaRecordEntity mediaRecord;

    @OneToOne
    ThreadEntity thread;

    @Column
    private Integer timeStampSeconds;

    @Column
    private Integer pageNumber;

    public ThreadMediaRecordReferenceEntity(ThreadEntity thread, MediaRecordEntity mediaRecord, int timeStamp, int pageNumber) {
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
        if (timeStampSeconds != null) {
            joiner.add("timeStampSeconds: " + timeStampSeconds);
        }
        if (pageNumber != null) {
            joiner.add("pageNumber: " + pageNumber);
        }
        return joiner.toString();
    }
}
