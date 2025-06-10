package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class QuestionThreadEntity extends ThreadEntity{
    public QuestionThreadEntity(ForumEntity forum, UUID creatorId, String title, @NotNull PostEntity question) {
        super(forum, creatorId, title);
        this.question = question;
    }

    @OneToOne
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    @NotNull
    PostEntity question;

    @OneToOne
    PostEntity answer;
}
