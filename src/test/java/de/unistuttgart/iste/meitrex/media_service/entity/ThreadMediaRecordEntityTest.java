package de.unistuttgart.iste.meitrex.media_service.entity;

import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadMediaRecordReferenceEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ThreadMediaRecordEntityTest {
    private final UUID courseId1 = UUID.randomUUID();

    @Test
    void testThreadMediaRecordToString(){
        QuestionThreadEntity questionThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .build();
        MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .build();
        int timestamp = 10;
        int pageNumber = 1;
        ThreadMediaRecordReferenceEntity threadMediaRecordReference =
                new ThreadMediaRecordReferenceEntity(questionThread, mediaRecord, timestamp, pageNumber);
        assertThat(threadMediaRecordReference.toString(), is("{ mediaRecordId: " + mediaRecord.getId()
                + ", threadId: " + questionThread.getId() + ", timeStampSeconds: 10, pageNumber: 1 }"));
    }

    @Test
    void testThreadMediaRecordEquals() {
        QuestionThreadEntity questionThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .build();
        MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .build();
        int timestamp = 10;
        int pageNumber = 1;
        ThreadMediaRecordReferenceEntity threadMediaRecordReference =
                new ThreadMediaRecordReferenceEntity(questionThread, mediaRecord, timestamp, pageNumber);
        ThreadMediaRecordReferenceEntity threadMediaRecordReference2 =
                new ThreadMediaRecordReferenceEntity(questionThread, mediaRecord, timestamp, pageNumber);
        assertThat(threadMediaRecordReference.equals(threadMediaRecordReference2), is(true));
    }
}
