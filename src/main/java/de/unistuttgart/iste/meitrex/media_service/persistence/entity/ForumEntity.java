package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "Forum")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumEntity implements Serializable {
    public ForumEntity(UUID courseId){
        this.courseId = courseId;
        threads = new ArrayList<>();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToMany
    List<ThreadEntity> threads;

    @Column(unique = true)
    UUID courseId;
}
