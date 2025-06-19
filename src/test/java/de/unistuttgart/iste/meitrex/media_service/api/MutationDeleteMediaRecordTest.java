package de.unistuttgart.iste.meitrex.media_service.api;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.MediaRecordDeletedEvent;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;
import de.unistuttgart.iste.meitrex.common.testutil.TablesToDelete;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;

@ContextConfiguration(classes = {MockMinIoClientConfiguration.class, MockTestPublisherConfiguration.class})
@TablesToDelete({"media_record_content_ids","media_record_course_ids", "media_record"})
@Transactional
@GraphQlApiTest
class MutationDeleteMediaRecordTest {

    @Autowired
    private MediaRecordRepository repository;

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = dummyCourseMembershipBuilder(courseId1);
    private final LoggedInUser.CourseMembership courseMembership2 = dummyCourseMembershipBuilder(courseId2);
    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Autowired
    private TopicPublisher topicPublisher;

    @BeforeEach
    void beforeEach() {
        reset(topicPublisher);
    }

    @Test
    void testDeleteMediaRecord(final GraphQlTester tester) {
        List<MediaRecordEntity> createdMediaRecords = fillRepositoryWithMediaRecordsAndCourseIds(repository, courseId1, courseId2);

        createdMediaRecords = repository.saveAll(createdMediaRecords);

        doNothing().when(topicPublisher).notifyMediaRecordDeleted(new MediaRecordDeletedEvent(createdMediaRecords.get(0).getId()));

        final String query = """
                mutation {
                    deleteMediaRecord(id: "%s")
                }
                """.formatted(createdMediaRecords.get(0).getId());

        tester.document(query)
                .execute()
                .path("deleteMediaRecord").entity(UUID.class).isEqualTo(createdMediaRecords.get(0).getId());

        // ensure that the media record left in the db is the other one (the one we didn't delete)
        assertThat(repository.count(), is((long) createdMediaRecords.size() - 1));
        final MediaRecordEntity remainingMediaRecord = repository.findAll().get(0);
        assertThat(remainingMediaRecord, equalTo(createdMediaRecords.get(1)));
    }
}
