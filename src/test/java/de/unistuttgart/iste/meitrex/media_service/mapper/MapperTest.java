package de.unistuttgart.iste.meitrex.media_service.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.InfoThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class MapperTest {
    ModelMapper modelMapper = new ModelMapper();
    ThreadMapper threadMapper = new ThreadMapper(modelMapper);

    @Test
    void testThreadMapper() {
        ThreadEntity questionThreadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .question(PostEntity.builder()
                        .id(UUID.randomUUID())
                        .creationTime(OffsetDateTime.now())
                        .content("Question Content")
                        .build())
                .posts(new ArrayList<>())
                .title("Question Title")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .build();
        ThreadEntity infoThreadEntity = InfoThreadEntity.builder()
                .id(UUID.randomUUID())
                .info(PostEntity.builder()
                        .id(UUID.randomUUID())
                        .creationTime(OffsetDateTime.now())
                        .content("Info Content")
                        .build())
                .posts(new ArrayList<>())
                .title("Info Title")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .build();
        assertThat(threadMapper.mapThread(questionThreadEntity), is(modelMapper.map(questionThreadEntity,
                QuestionThread.class)));
        assertThat(threadMapper.mapThread(infoThreadEntity), is(modelMapper.map(infoThreadEntity,
                InfoThread.class)));
    }

}
