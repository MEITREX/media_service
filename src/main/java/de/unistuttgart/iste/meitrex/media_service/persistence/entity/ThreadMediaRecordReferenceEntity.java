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

    @ManyToOne
    @MapsId("threadId")
    @JoinColumn(name = "thread_id")
    ThreadEntity thread;

    @Column
    private int timeStampSeconds;

    @Column
    private int pageNumber;
}
