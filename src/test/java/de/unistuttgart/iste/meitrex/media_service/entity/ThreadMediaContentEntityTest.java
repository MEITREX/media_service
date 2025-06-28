package de.unistuttgart.iste.meitrex.media_service.entity;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadContentReferenceEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ThreadMediaContentEntityTest {

    @Test
    void testThreadContentToString(){
        QuestionThreadEntity questionThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .build();
        MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .build();
        int timestamp = 10;
        int pageNumber = 1;
        UUID contentId = UUID.randomUUID();
        ThreadContentReferenceEntity threadContentReference =
                new ThreadContentReferenceEntity(questionThread, contentId, timestamp, pageNumber);
        assertThat(threadContentReference.toString(), is("{ contentId: " + contentId
                + ", threadId: " + questionThread.getId() + ", timeStampSeconds: 10, pageNumber: 1 }"));
    }

    @Test
    void testThreadContentEquals() {
        QuestionThreadEntity questionThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .build();
        MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .build();
        int timestamp = 10;
        int pageNumber = 1;
        UUID contentId = UUID.randomUUID();
        ThreadContentReferenceEntity threadContentReference =
                new ThreadContentReferenceEntity(questionThread, contentId, timestamp, pageNumber);
        ThreadContentReferenceEntity threadContentReference2 =
                new ThreadContentReferenceEntity(questionThread, contentId, timestamp, pageNumber);
        assertThat(threadContentReference.equals(threadContentReference2), is(true));
    }
}
