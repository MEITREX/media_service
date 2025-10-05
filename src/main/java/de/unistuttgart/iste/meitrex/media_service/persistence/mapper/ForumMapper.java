package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.Forum;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ForumEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ForumMapper {
    private final ThreadMapper threadMapper;

    public Forum forumEntityToForum(ForumEntity entity) {
        return Forum.builder()
                .setCourseId(entity.getCourseId())
                .setId(entity.getId())
                .setUserIds(List.copyOf(entity.getUserIds()))
                .setThreads(entity.getThreads().stream().map(threadMapper::mapThread).toList())
                .build();
    }
}
