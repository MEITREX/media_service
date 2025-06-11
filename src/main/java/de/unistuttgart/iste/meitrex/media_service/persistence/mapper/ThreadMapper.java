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
    private final ModelMapper modelMapper;

    public Thread mapThread(ThreadEntity threadEntity) {
        if (threadEntity instanceof QuestionThreadEntity questionThread) {
            return modelMapper.map(questionThread, QuestionThread.class);
        } else if (threadEntity instanceof InfoThreadEntity infoThread) {
            return modelMapper.map(infoThread, InfoThread.class);
        } else {
            throw new ClassCastException("Thread Entity is in wrong class " + threadEntity.getClass().getName());
        }
    }
}
