package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Result;
import de.unistuttgart.iste.meitrex.generated.dto.Status;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionExercise;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionSolution;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
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
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@GraphQlApiTest
public class MutationUploadSolutionTest {
    @Autowired
    private SubmissionExerciseRepository  submissionExerciseRepository;

    @Autowired
    private MinioClient minioClient;

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);
    private final LoggedInUser.CourseMembership courseMembership2 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId2);


    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Test
    void testUploadSolution(final HttpGraphQlTester tester)throws  Exception {
        UUID assessmentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(createdAt);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseRepository.save(submissionExerciseEntity);



        final String query = """
                mutation {
                    uploadSolution(
                        solution: {
                            courseId: "%s",
                            submissionExerciseId: "%s"
                        }) {
                        id,
                        userId,
                        submissionDate,
                        result {
                            id,
                            status
                        }
                    }
                }
        """.formatted(courseId1, submissionExerciseEntity.getId());
        final SubmissionSolution submissionSolution = tester.document(query)
                .execute()
                .path("uploadSolution").entity(SubmissionSolution.class).get();
        assertThat(submissionSolution.getResult().getStatus(), is(Status.pending));
        assertThat(submissionSolution.getUserId(), is(currentUser.getId()));
        assertThat((double) submissionSolution.getSubmissionDate().toInstant().toEpochMilli(),
                closeTo((double) createdAt.toInstant().toEpochMilli(), 100000.0));
    }
}
