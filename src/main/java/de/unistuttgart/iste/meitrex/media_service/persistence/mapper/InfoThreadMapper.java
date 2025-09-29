package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.ThreadContentReference;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.InfoThreadEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InfoThreadMapper {
    private final PostMapper postMapper;
    private final ModelMapper modelMapper;

    public InfoThread mapInfoThread(InfoThreadEntity infoThreadEntity) {
        InfoThread infoThread = InfoThread.builder()
                .setId(infoThreadEntity.getId())
                .setCreatorId(infoThreadEntity.getCreatorId())
                .setTitle(infoThreadEntity.getTitle())
                .setCreationTime(infoThreadEntity.getCreationTime())
                .setPosts(postMapper.mapToPosts(infoThreadEntity.getPosts()))
                .setNumberOfPosts(infoThreadEntity.getNumberOfPosts())
                .setThreadContentReference(null)
                .setInfo(postMapper.mapToPost(infoThreadEntity.getInfo()))
                .build();
        if (infoThreadEntity.getThreadContentReference() != null) {
            infoThread.setThreadContentReference(modelMapper.map(infoThreadEntity.getThreadContentReference(), ThreadContentReference.class));
        }
        return infoThread;
    }
}
