package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.InfoThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ThreadEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThreadMapper {
    private final InfoThreadMapper infoThreadMapper;
    private final QuestionThreadMapper questionThreadMapper;

    public Thread mapThread(ThreadEntity threadEntity) {
        switch (threadEntity) {
            case QuestionThreadEntity questionThread -> {
                return questionThreadMapper.mapQuestionThread(questionThread);
            }
            case InfoThreadEntity infoThreadEntity -> {
                return infoThreadMapper.mapInfoThread(infoThreadEntity);
            }
            default -> throw new IllegalArgumentException("Unknown thread: " + threadEntity);
        }
    }
}
