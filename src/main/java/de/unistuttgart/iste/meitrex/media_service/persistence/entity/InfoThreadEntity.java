package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@NoArgsConstructor
public class InfoThreadEntity extends ThreadEntity{
    public InfoThreadEntity(ForumEntity forum, UUID creatorId, String title, @NotNull PostEntity info) {
        super(forum, creatorId, title);
        this.info = info;
    }

    @OneToOne
    @NotNull
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    PostEntity info;
}
