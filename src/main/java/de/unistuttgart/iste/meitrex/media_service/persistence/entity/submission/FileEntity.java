package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity(name = "File")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntity implements IWithId<UUID> {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 500)
    private String uploadUrl;

    @Column(length = 500)
    private String downloadUrl;

    @Column
    private String name;
}
