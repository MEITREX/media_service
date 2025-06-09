package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import de.unistuttgart.iste.meitrex.media_service.persistence.embeddable.ThreadMediaRecordReferenceKey;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.PackagePrivate;

import java.io.Serializable;

@Entity(name = "ThreadMediaRecordReference")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThreadMediaRecordReferenceEntity implements Serializable {

    @EmbeddedId
    ThreadMediaRecordReferenceKey id;

    @ManyToOne
    @MapsId("mediaRecordId")
    @JoinColumn(name = "media_record_id")
    MediaRecordEntity mediaRecord;

    @OneToOne
    @MapsId("threadId")
    @JoinColumn(name = "thread_id")
    ThreadEntity thread;

    @Column
    private int timeStampSeconds;

    @Column
    private int pageNumber;

    public ThreadMediaRecordReferenceEntity(ThreadEntity thread, MediaRecordEntity mediaRecord, int timeStamp, int pageNumber) {
        this.thread = thread;
        this.mediaRecord = mediaRecord;
        this.timeStampSeconds = timeStamp;
        this.pageNumber = pageNumber;
    }
}
