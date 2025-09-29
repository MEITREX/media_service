package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.InfoThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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
