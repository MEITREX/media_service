package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity(name = "Task")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity implements IWithId<UUID> {

    @Id
    private UUID itemId;

    @Column
    private String name;

    @Column
    private int maxScore;

    @Override
    public UUID getId() {
        return itemId;
    }
}
