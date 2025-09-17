package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    private String uploadUrl;

    private String downloadUrl;

    private String name;
}
