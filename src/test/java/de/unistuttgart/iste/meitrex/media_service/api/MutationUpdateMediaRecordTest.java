package de.unistuttgart.iste.meitrex.media_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil.dummyCourseMembershipBuilder;
import static de.unistuttgart.iste.meitrex.media_service.test_util.MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@TablesToDelete({"media_record_content_ids","media_record_course_ids", "media_record"})
@Transactional
@GraphQlApiTest
class MutationUpdateMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    private UUID courseId1 = UUID.randomUUID();

    private LoggedInUser.CourseMembership courseMembership = dummyCourseMembershipBuilder(courseId1);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership);

    @Test
    void testUpdateMediaRecord(final GraphQlTester tester) {
        List<MediaRecordEntity> expectedMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, UUID.randomUUID());

        expectedMediaRecords = repository.saveAll(expectedMediaRecords);

        final UUID newContentId = UUID.randomUUID();
        final String query = """
                mutation {
                    updateMediaRecord: updateMediaRecord(input: {
                        id: "%s",
                        name: "Updated Record",
                        type: URL,
                        contentIds: ["%s"]
                    }) {
                        id,
                        name,
                        type,
                        contentIds
                    }
                }
                """.formatted(expectedMediaRecords.get(0).getId(), newContentId);

        tester.document(query)
                .execute()
                .path("updateMediaRecord.id").entity(UUID.class).isEqualTo(expectedMediaRecords.get(0).getId())
                .path("updateMediaRecord.name").entity(String.class).isEqualTo("Updated Record")
                .path("updateMediaRecord.type").entity(MediaRecordEntity.MediaType.class).isEqualTo(MediaRecordEntity.MediaType.URL)
                .path("updateMediaRecord.contentIds").entityList(UUID.class).hasSize(1).contains(newContentId);

        assertThat(repository.count(), is((long)expectedMediaRecords.size()));
        // check that the other record in the repository hasn't changed
        final var actual = repository.findById(expectedMediaRecords.get(1).getId()).get();
        assertThat(actual, is(expectedMediaRecords.get(1)));
        // get the updated record and check that it has been updated
        final MediaRecordEntity actualUpdatedRecord = repository.findById(expectedMediaRecords.get(0).getId()).get();

        assertThat(actualUpdatedRecord.getName(), is("Updated Record"));
        assertThat(actualUpdatedRecord.getType(), is(MediaRecordEntity.MediaType.URL));
        assertThat(actualUpdatedRecord.getContentIds(), contains(newContentId));
    }

}
