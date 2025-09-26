package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionExercise;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
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

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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
@GraphQlApiTest
public class MutationCreateSubmissionExerciseTest {
    @Autowired
    private SubmissionExerciseRepository  submissionExerciseRepository;

    @Autowired
    private MinioClient minioClient;

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.COURSE_CREATOR));

    @Test
    void testCreateSubmissionExercise(final HttpGraphQlTester tester)throws  Exception {
        UUID courseId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        final String query = """
                mutation {
                    _internal_noauth_createSubmissionExercise(
                        courseId: "%s",
                        assessmentId: "%s",
                        input: {
                            endDate: "%s",
                            name: "Test name"
                        }) {
                        assessmentId,
                        courseId,
                        endDate,
                        name
                    }
                }
        """.formatted(courseId, assessmentId, createdAt);
        final SubmissionExercise submissionExercise = tester.document(query)
                .execute()
                .path("_internal_noauth_createSubmissionExercise").entity(SubmissionExercise.class).get();
        assertThat(submissionExercise.getCourseId(), is(courseId));
        assertThat(submissionExercise.getName(), is("Test name"));
        assertThat(submissionExercise.getAssessmentId(), is(assessmentId));
        assertThat(submissionExercise.getEndDate().toInstant().truncatedTo(ChronoUnit.MILLIS),
                is(createdAt.toInstant().truncatedTo(ChronoUnit.MILLIS)));
    }
}
