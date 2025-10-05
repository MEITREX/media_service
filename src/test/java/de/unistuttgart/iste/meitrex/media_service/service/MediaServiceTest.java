package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ServerSource;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import io.minio.MinioClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@Configuration
@EnableAsync
class MediaServiceTest {

    private final MediaRecordRepository repository =
            mock(MediaRecordRepository.class);

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

        assertThat(actual.getId(), is(entity.getId()));
        assertThat(actual.getCreatorId(), is(entity.getCreatorId()));
        assertThat(actual.getContentIds(), is(entity.getContentIds()));
        assertThat(actual.getProgressData(), is(entity.getProgressData()));
    }

    @Test
    void testGetMediaRecordByIdWithCourseIds() {

        final MediaRecordEntity entity = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .courseIds(List.of(UUID.randomUUID()))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .build();

        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        final MediaRecordEntity actual = mapper.map(service.getMediaRecordById(entity.getId()), MediaRecordEntity.class);

        assertThat(actual.getId(), is(entity.getId()));
        assertThat(actual.getCreatorId(), is(entity.getCreatorId()));
        assertThat(actual.getCourseIds(), is(entity.getCourseIds()));
        assertThat(actual.getContentIds(), is(entity.getContentIds()));
        assertThat(actual.getProgressData(), is(entity.getProgressData()));
    }

    @Test
    void TestPublishMediaRecordFile() {
        UUID mediaId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(mediaId).name("Lecture.pdf")
                .courseIds(List.of(courseId))
                .contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).type(MediaRecordEntity.MediaType.DOCUMENT).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(mediaId);
        when(repository.findContentIdsByMediaRecordId(mediaId)).thenReturn(List.of());

        service.publishMaterialPublishedEvent(mediaId);

        verify(topicPublisher).notificationEvent(
                eq(courseId), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + courseId + "/media?selectedDocument=" + mediaId),
                eq("New Material is uploaded!"),
                eq("material: Lecture.pdf")
        );
    }

    @Test
    void TestPublishMediaRecordFile_unnamed() {
        UUID mid = UUID.randomUUID(), cid = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder().id(mid).name(null)
                .courseIds(List.of(cid)).contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).type(MediaRecordEntity.MediaType.DOCUMENT).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(mid);
        when(repository.findContentIdsByMediaRecordId(mid)).thenReturn(List.of());

        service.publishMaterialPublishedEvent(mid);

        verify(topicPublisher).notificationEvent(
                eq(cid), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + cid + "/media?selectedDocument=" + mid),
                eq("New Material is uploaded!"),
                eq("material: Unnamed File")
        );
    }

    @Test
    void TestPublishMediaRecordFile_video() {
        UUID mediaId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(mediaId).name("Video.mp4")
                .courseIds(List.of(courseId))
                .contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).type(MediaRecordEntity.MediaType.VIDEO).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(mediaId);
        when(repository.findContentIdsByMediaRecordId(mediaId)).thenReturn(List.of());

        service.publishMaterialPublishedEvent(mediaId);

        verify(topicPublisher).notificationEvent(
                eq(courseId), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + courseId + "/media?selectedVideo=" + mediaId),
                eq("New Material is uploaded!"),
                eq("material: Video.mp4")
        );
    }

    @Test
    void TestPublishMediaRecordFile_withContentId_document() {
        UUID mediaId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID latestContentId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(mediaId).name("Doc.pdf")
                .courseIds(List.of(courseId))
                .contentIds(List.of(latestContentId))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .type(MediaRecordEntity.MediaType.DOCUMENT).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(mediaId);
        when(repository.findContentIdsByMediaRecordId(mediaId)).thenReturn(List.of(latestContentId));

        service.publishMaterialPublishedEvent(mediaId);

        verify(topicPublisher).notificationEvent(
                eq(courseId), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + courseId + "/media/" + latestContentId + "?selectedDocument=" + mediaId),
                eq("New Material is uploaded!"),
                eq("material: Doc.pdf")
        );
    }

    @Test
    void TestPublishMediaRecordFile_withContentId_video() {
        UUID mediaId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID latestContentId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(mediaId).name("Clip.mp4")
                .courseIds(List.of(courseId))
                .contentIds(List.of(latestContentId))
                .creatorId(UUID.randomUUID())
                .progressData(List.of())
                .type(MediaRecordEntity.MediaType.VIDEO).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(mediaId);
        when(repository.findContentIdsByMediaRecordId(mediaId)).thenReturn(List.of(latestContentId));

        service.publishMaterialPublishedEvent(mediaId);

        verify(topicPublisher).notificationEvent(
                eq(courseId), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + courseId + "/media/" + latestContentId + "?selectedVideo=" + mediaId),
                eq("New Material is uploaded!"),
                eq("material: Clip.mp4")
        );
    }

    @Test
    void TestPublishMediaRecordFile_notFound() {
        UUID mediaId = UUID.randomUUID();
        doReturn(Optional.empty()).when(repository).findWithCoursesById(mediaId);
        assertThrows(IllegalArgumentException.class, () -> service.publishMaterialPublishedEvent(mediaId));
    }

    @Test
    void TestGetMediaRecordsForUser() {
        UUID userId = UUID.randomUUID();
        MediaRecordEntity e1 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("A")
                .creatorId(userId).contentIds(List.of()).courseIds(List.of()).progressData(List.of()).build();
        MediaRecordEntity e2 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("B")
                .creatorId(userId).contentIds(List.of()).courseIds(List.of()).progressData(List.of()).build();
        when(repository.findMediaRecordEntitiesByCreatorId(userId)).thenReturn(List.of(e1, e2));

        var list = service.getMediaRecordsForUser(userId);
        var r1 = mapper.map(list.get(0), MediaRecordEntity.class);
        var r2 = mapper.map(list.get(1), MediaRecordEntity.class);

        assertThat(r1.getId(), is(e1.getId()));
        assertThat(r2.getId(), is(e2.getId()));
        assertThat(list.size(), is(2));
    }

    @Test
    void TestGetMediaRecordsForUsers() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        MediaRecordEntity e1 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("A")
                .creatorId(u1).contentIds(List.of()).courseIds(List.of()).progressData(List.of()).build();
        MediaRecordEntity e2 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("B")
                .creatorId(u1).contentIds(List.of()).courseIds(List.of()).progressData(List.of()).build();

        when(repository.findMediaRecordEntitiesByCreatorId(u1)).thenReturn(List.of(e1, e2));
        when(repository.findMediaRecordEntitiesByCreatorId(u2)).thenReturn(List.of());

        var result = service.getMediaRecordsForUsers(List.of(u1, u2));
        assertThat(result.size(), is(2));

        var u1List = result.get(0);
        var a = mapper.map(u1List.get(0), MediaRecordEntity.class);
        var b = mapper.map(u1List.get(1), MediaRecordEntity.class);
        assertThat(a.getId(), is(e1.getId()));
        assertThat(b.getId(), is(e2.getId()));

        var u2List = result.get(1);
        assertThat(u2List.size(), is(0));
    }

    @Test
    void TestGetMediaRecordEntitiesByContentId() {
        UUID contentId = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder().id(UUID.randomUUID()).name("X")
                .contentIds(List.of(contentId)).creatorId(UUID.randomUUID()).courseIds(List.of()).progressData(List.of()).build();

        when(repository.findMediaRecordEntitiesByContentIds(List.of(contentId))).thenReturn(List.of(e));

        var list = service.getMediaRecordEntitiesByContentId(contentId);
        assertThat(list.size(), is(1));
        assertThat(list.get(0).getId(), is(e.getId()));
    }

    @Test
    void TestGetMediaRecordsForCourses() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();

        MediaRecordEntity e1 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("E1")
                .courseIds(List.of(c1)).contentIds(List.of()).creatorId(UUID.randomUUID()).progressData(List.of()).build();
        MediaRecordEntity e2 = MediaRecordEntity.builder().id(UUID.randomUUID()).name("E2")
                .courseIds(List.of(c1, c2)).contentIds(List.of()).creatorId(UUID.randomUUID()).progressData(List.of()).build();

        when(repository.findMediaRecordEntitiesByCourseIds(List.of(c1, c2))).thenReturn(List.of(e1, e2));

        var result = service.getMediaRecordsForCourses(List.of(c1, c2));
        assertThat(result.size(), is(2));

        var listForC1 = result.get(0);
        var listForC2 = result.get(1);

        var c1Ids = listForC1.stream().map(x -> mapper.map(x, MediaRecordEntity.class).getId()).toList();
        var c2Ids = listForC2.stream().map(x -> mapper.map(x, MediaRecordEntity.class).getId()).toList();

        assertThat(c1Ids.contains(e1.getId()), is(true));
        assertThat(c1Ids.contains(e2.getId()), is(true));
        assertThat(c2Ids.contains(e2.getId()), is(true));
        assertThat(c2Ids.contains(e1.getId()), is(false));
    }

    @Test
    void TestRemoveExpiredUrls_private_expired() throws Exception {
        var m = service.getClass().getDeclaredMethod("getMediaRecordsForUser", UUID.class).getGenericReturnType();
        var mediaRecordClass = (Class<?>) ((ParameterizedType) m).getActualTypeArguments()[0];
        Object record = mediaRecordClass.getDeclaredConstructor().newInstance();
        var setD = mediaRecordClass.getMethod("setDownloadUrl", String.class);
        var setU = mediaRecordClass.getMethod("setUploadUrl", String.class);
        var setS = mediaRecordClass.getMethod("setStandardizedDownloadUrl", String.class);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);
        String past = f.format(Instant.now().minusSeconds(3600));
        String expired = "https://x?X-Amz-Date=" + past + "&X-Amz-Expires=60";
        setD.invoke(record, expired);
        setU.invoke(record, expired);
        setS.invoke(record, expired);
        Method rm = service.getClass().getDeclaredMethod("removeExpiredUrlsFromMediaRecord", mediaRecordClass);
        rm.setAccessible(true);
        Object out = rm.invoke(service, record);
        var getD = mediaRecordClass.getMethod("getDownloadUrl");
        var getU = mediaRecordClass.getMethod("getUploadUrl");
        var getS = mediaRecordClass.getMethod("getStandardizedDownloadUrl");
        assertThat(getD.invoke(out) == null, is(true));
        assertThat(getU.invoke(out) == null, is(true));
        assertThat(getS.invoke(out) == null, is(true));
    }

    @Test
    void TestIsExpired_private() throws Exception {
        Method isExp = service.getClass().getDeclaredMethod("isExpired", String.class);
        isExp.setAccessible(true);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);
        String past = f.format(Instant.now().minusSeconds(7200));
        String future = f.format(Instant.now());
        String expired = "https://x?X-Amz-Date=" + past + "&X-Amz-Expires=60";
        String notExpired = "https://x?X-Amz-Date=" + future + "&X-Amz-Expires=7200";
        boolean e1 = (Boolean) isExp.invoke(service, expired);
        boolean e2 = (Boolean) isExp.invoke(service, notExpired);
        assertThat(e1, is(true));
        assertThat(e2, is(false));
    }

    @Test
    void TestPublishMediaRecordFile_blankName() {
        UUID id = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(id).name("   ")
                .courseIds(List.of(cid))
                .contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).type(MediaRecordEntity.MediaType.DOCUMENT).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(id);
        when(repository.findContentIdsByMediaRecordId(id)).thenReturn(List.of());

        service.publishMaterialPublishedEvent(id);

        verify(topicPublisher).notificationEvent(
                eq(cid), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + cid + "/media?selectedDocument=" + id),
                eq("New Material is uploaded!"),
                eq("material: Unnamed File")
        );
    }

    @Test
    void TestPublishMediaRecordFile_contentIdsNull() {
        UUID id = UUID.randomUUID();
        UUID cid = UUID.randomUUID();
        MediaRecordEntity e = MediaRecordEntity.builder()
                .id(id).name("N.pdf")
                .courseIds(List.of(cid))
                .contentIds(List.of()).creatorId(UUID.randomUUID())
                .progressData(List.of()).type(MediaRecordEntity.MediaType.DOCUMENT).build();

        doReturn(Optional.of(e)).when(repository).findWithCoursesById(id);
        when(repository.findContentIdsByMediaRecordId(id)).thenReturn(null);

        service.publishMaterialPublishedEvent(id);

        verify(topicPublisher).notificationEvent(
                eq(cid), isNull(), eq(ServerSource.MEDIA),
                eq("/courses/" + cid + "/media?selectedDocument=" + id),
                eq("New Material is uploaded!"),
                eq("material: N.pdf")
        );
    }

    @Test
    void publishMaterialPublishedEventWithDelay_shouldNotPropagate_whenInnerThrows() throws Exception {
        UUID id = UUID.randomUUID();

        MediaService spyService = spy(service);

        CountDownLatch invoked = new CountDownLatch(1);

        doAnswer(invocation -> {
            invoked.countDown();
            throw new RuntimeException("boom");
        }).when(spyService).publishMaterialPublishedEvent(id);

        assertDoesNotThrow(() -> spyService.publishMaterialPublishedEventWithDelay(id));

        boolean called = invoked.await(7, TimeUnit.SECONDS);
        org.junit.jupiter.api.Assertions.assertTrue(called, "timeout waiting for async invocation");

        verify(spyService, times(1)).publishMaterialPublishedEvent(id);
    }
}
