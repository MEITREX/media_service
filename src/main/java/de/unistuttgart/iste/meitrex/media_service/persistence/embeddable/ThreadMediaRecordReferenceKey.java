package de.unistuttgart.iste.meitrex.media_service.persistence.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreadMediaRecordReferenceKey implements Serializable {
    @Column(name = "media_record_id")
    private UUID mediaRecordId;

    @Column(name = "thread_id")
    private UUID threadId;
}