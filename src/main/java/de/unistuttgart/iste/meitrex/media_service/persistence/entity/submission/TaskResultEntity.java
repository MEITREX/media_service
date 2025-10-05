package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity (name= "TaskResult")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskResultEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private UUID itemId;

    @Column
    private Integer requiredScore;

    @Column
    private Integer score;

    public TaskResultEntity(UUID itemId, int requiredScore, int score) {
        this.itemId = itemId;
        this.requiredScore = requiredScore;
        this.score = score;
    }
}
