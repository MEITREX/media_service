package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ServerSource;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import io.minio.MinioClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MediaServiceTest {

    private final MediaRecordRepository repository = mock(MediaRecordRepository.class);

    private final MinioClient mockMinIoClient = new MockMinIoClientConfiguration().getTestMinIoClient();

    private final ModelMapper mapper = new ModelMapper();

    private final TopicPublisher topicPublisher = mock(TopicPublisher.class);

    private final FileConversionService fileConversionService = mock(FileConversionService.class);

    private final MediaService service = new MediaService(mockMinIoClient, mockMinIoClient, topicPublisher, repository,
            mapper, fileConversionService);


    MediaServiceTest() throws Exception {
        // constructor with exception required because of min io mock
    }

    @Test
    void testRequireMediaRecordExisting() {
        final MediaRecordEntity entity = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .build();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThat(service.requireMediaRecordExisting(entity.getId()), is(entity));

        final UUID notExistingId = UUID.randomUUID();
        assertThrows(EntityNotFoundException.class, () -> service.requireMediaRecordExisting(notExistingId));
    }

    @Test
    void testGetMediaRecordById() {

        final MediaRecordEntity entity = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .build();

        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        final MediaRecordEntity actual = mapper.map(service.getMediaRecordById(entity.getId()), MediaRecordEntity.class);

        assertThat(actual, is(entity));
    }

    @Test
    void testGetMediaRecordByIdWithCourseIds() {
        final List<UUID> courseIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        final MediaRecordEntity entity = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .courseIds(courseIds)
                .contentIds(List.of(UUID.randomUUID()))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .build();

        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        final MediaRecordEntity actual = mapper.map(service.getMediaRecordById(entity.getId()), MediaRecordEntity.class);

        assertThat(actual, is(entity));
    }

    @Test
    void TestPublishMediaRecordFile() {
        UUID mediaId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(mediaId).name("Lecture.pdf")
                .courseIds(List.of(courseId))
                .contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).build();
        when(repository.getReferenceById(mediaId)).thenReturn(e);

        service.publishMediaRecordFileCreatedEvent(mediaId);

        String link = "/courses/" + courseId + "/materials/" + mediaId;
        verify(topicPublisher).notifyMediaRecordFileCreated(any());
        verify(topicPublisher).notificationEvent(
                eq(courseId), isNull(), eq(ServerSource.MEDIA),
                eq(link), eq("New Material is uploaded!"), eq("material:Lecture.pdf"));
    }

    @Test
    void TestPublishMediaRecordFile_unnamed() {
        UUID mid = UUID.randomUUID(), cid = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder().id(mid).name(null)
                .courseIds(List.of(cid)).contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).build();
        when(repository.getReferenceById(mid)).thenReturn(e);
        service.publishMediaRecordFileCreatedEvent(mid);
        String link = "/courses/" + cid + "/materials/" + mid;
        verify(topicPublisher).notificationEvent(eq(cid), isNull(), eq(ServerSource.MEDIA),
                eq(link), eq("New Material is uploaded!"), eq("material:Unnamed File"));
    }
}