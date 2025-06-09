package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
public class InfoThreadEntity extends ThreadEntity{
    public InfoThreadEntity(ForumEntity forum, UUID creatorId, String title) {
        super(forum, creatorId, title);
    }
}
