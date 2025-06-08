package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

import java.util.UUID;

@Entity(name = "Forum")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumEntity {
    public ForumEntity(UUID courseID){
        this.courseId = courseID;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToMany
    List<ThreadEntity> threads;

    @Column
    UUID courseId;
}
