package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Result;
import de.unistuttgart.iste.meitrex.generated.dto.Status;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@GraphQlApiTest
public class MutationUpdateResultTest {
    @Autowired
    private SubmissionExerciseRepository  submissionExerciseRepository;

    @Autowired
    private SubmissionExerciseSolutionRepository  submissionExerciseSolutionRepository;

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

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setItemId(UUID.randomUUID());
        taskEntity.setMaxScore(100);
        submissionExerciseEntity.getTasks().add(taskEntity);
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);

        ExerciseSolutionEntity exerciseSolutionEntity = new ExerciseSolutionEntity();
        exerciseSolutionEntity.setUserId(currentUser.getId());
        exerciseSolutionEntity.setSubmissionDate(OffsetDateTime.now());
        exerciseSolutionEntity.setFiles(new ArrayList<>());
        exerciseSolutionEntity.setResult(initialResultEntity(currentUser.getId(), submissionExerciseEntity.getTasks()));
        submissionExerciseEntity.getSolutions().add(exerciseSolutionEntity);
        submissionExerciseSolutionRepository.save(exerciseSolutionEntity);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                mutation {
                    updateResult(
                        result: {
                            id: "%s",
                            assessmentId: "%s",
                            courseId: "%s",
                            status: %s,
                            results: [
                                {
                                    itemId: "%s",
                                    score: %s
                                }
                            ]
                        }) {
                        id,
                        status,
                        results {
                            itemId,
                            score
                        }
                    }
                }
        """.formatted(exerciseSolutionEntity.getResult().getId(), assessmentId, courseId1, Status.passed,
                submissionExerciseEntity.getTasks().get(0).getId(), 100);
        final Result result = tester.document(query)
                .execute()
                .path("updateResult").entity(Result.class).get();
        assertThat(result.getStatus(), is(Status.passed));
        assertThat(result.getResults().size(), is(1));
        assertThat(result.getResults().get(0).getScore(), is(100));
        assertThat(result.getResults().get(0).getItemId(), is(submissionExerciseEntity.getTasks().get(0).getId()));
    }

    private ResultEntity initialResultEntity(UUID userId, List<TaskEntity> tasks) {
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setStatus(ResultEntity.Status.pending);
        resultEntity.setResults(new ArrayList<>());
        resultEntity.setUserId(userId);
        tasks.forEach(taskEntity -> resultEntity.getResults()
                .add(new TaskResultEntity(taskEntity.getId(), taskEntity.getMaxScore(), 0)));
        return resultEntity;
    }
}
