package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "MediaRecord")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecordEntity implements IWithId<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID creatorId;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MediaType type;

    @ElementCollection
    private List<UUID> contentIds;

    @Column(length = 500)
    private String uploadUrl;

    @Column(length = 500)
    private String downloadUrl;

    @Column(length = 500, nullable = true)
    private String standardizedDownloadUrl;

    @ElementCollection
    private List<UUID> courseIds;

    @OneToMany(mappedBy = "primaryKey.mediaRecordId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    private List<MediaRecordProgressDataEntity> progressData = new ArrayList<>();

    @OneToMany(mappedBy = "mediaRecord")
    private List<ThreadMediaRecordReferenceEntity> threadMediaRecordReference;

    public enum MediaType {
        AUDIO,
        VIDEO,
        IMAGE,
        PRESENTATION,
        DOCUMENT,
        URL
    }
}
