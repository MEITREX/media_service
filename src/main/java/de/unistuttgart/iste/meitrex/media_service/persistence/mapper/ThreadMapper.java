package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.InfoThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ThreadEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThreadMapper {
    private final ModelMapper modelMapper;

    public Thread mapThread(ThreadEntity threadEntity) {
        switch (threadEntity) {
            case QuestionThreadEntity questionThread -> {
                return modelMapper.map(questionThread, QuestionThread.class);
            }
            case InfoThreadEntity infoThreadEntity -> {
                return modelMapper.map(infoThreadEntity, InfoThread.class);
            }
            default -> throw new IllegalArgumentException("Unknown thread: " + threadEntity);
        }
    }
}
