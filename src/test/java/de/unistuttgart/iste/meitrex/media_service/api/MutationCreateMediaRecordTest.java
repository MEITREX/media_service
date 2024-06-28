package de.unistuttgart.iste.meitrex.media_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@TablesToDelete({"media_record_content_ids","media_record_course_ids", "media_record"})
@GraphQlApiTest
class MutationCreateMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    @Autowired
    private MinioClient minioClient;

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.COURSE_CREATOR));

    @Test
    void testCreateMediaRecord(final HttpGraphQlTester tester) throws Exception {
        final UUID userId1 = currentUser.getId();

        final String query = """
                mutation {
                    createMediaRecord(input: {
                        name: "Example Record",
                        type: VIDEO,
                        contentIds: ["e8653f6f-9c14-4d84-8942-613ec651153a"]
                    }) {
                        id,
                        name,
                        creatorId,
                        type,
                        contentIds,
                        uploadUrl,
                        downloadUrl
                    }
                }
                """;

        final UUID id = tester.document(query)
                .execute()
                .path("createMediaRecord.name").entity(String.class).isEqualTo("Example Record")
                .path("createMediaRecord.creatorId").entity(UUID.class).isEqualTo(userId1)
                .path("createMediaRecord.type").entity(String.class).isEqualTo("VIDEO")
                .path("createMediaRecord.contentIds").entityList(UUID.class)
                    .containsExactly(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a"))
                .path("createMediaRecord.uploadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("createMediaRecord.downloadUrl").entity(String.class).isEqualTo("http://example.com")
                .path("createMediaRecord.id").entity(UUID.class).get();

        assertThat(repository.count(), is(1L));
        final var mediaRecord = repository.findAll().get(0);
        assertThat(mediaRecord.getId(), is(id));
        assertThat(mediaRecord.getName(), is("Example Record"));
        assertThat(mediaRecord.getCreatorId(), is(userId1));
        assertThat(mediaRecord.getType(), Matchers.is(MediaRecordEntity.MediaType.VIDEO));
        assertThat(mediaRecord.getContentIds(), contains(UUID.fromString("e8653f6f-9c14-4d84-8942-613ec651153a")));

        verify(minioClient).getPresignedObjectUrl(GetPresignedObjectUrlArgs
                .builder()
                .method(Method.PUT)
                .bucket("video")
                .object(id.toString())
                .expiry(15, TimeUnit.MINUTES)
                .build());

        verify(minioClient).getPresignedObjectUrl(GetPresignedObjectUrlArgs
                .builder()
                .method(Method.GET)
                .bucket("video")
                .object(id.toString())
                .expiry(15, TimeUnit.MINUTES)
                .build());
    }

}
