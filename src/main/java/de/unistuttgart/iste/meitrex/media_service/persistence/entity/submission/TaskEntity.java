package de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission;

import jakarta.persistence.*;

import java.util.UUID;

@Entity(name = "Task")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private int maxScore;

    @Column
    SkillLevel skillLevel;

    @Column
    KnowledgeArea knowledgeArea;
}
