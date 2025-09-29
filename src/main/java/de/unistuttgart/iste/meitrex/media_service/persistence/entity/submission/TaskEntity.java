package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private String name;

    @Column
    @NotNull
    private Integer number;

    @Column
    @NotNull
    private int maxScore;

    @Override
    public UUID getId() {
        return itemId;
    }
}
