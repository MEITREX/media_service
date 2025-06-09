package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.UUID;

@Entity
@NoArgsConstructor
public class QuestionThreadEntity extends ThreadEntity{
    public QuestionThreadEntity(ForumEntity forum, UUID creatorId, String title) {
        super(forum, creatorId, title);
    }
}
