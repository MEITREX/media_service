package de.unistuttgart.iste.meitrex.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.*;

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
        userIds = new HashSet<>();
    }
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @OneToMany
    List<ThreadEntity> threads;

    @Column(unique = true)
    UUID courseId;

    @ElementCollection
    Set<UUID> userIds;
}
