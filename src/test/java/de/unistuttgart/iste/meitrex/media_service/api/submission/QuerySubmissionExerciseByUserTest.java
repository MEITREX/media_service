package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Status;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionExercise;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionSolution;
import de.unistuttgart.iste.meitrex.generated.dto.Task;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@GraphQlApiTest
public class QuerySubmissionExerciseByUserTest {
    @Autowired
    private SubmissionExerciseRepository submissionExerciseRepository;

    @Autowired
    private ExerciseSolutionRepository exerciseSolutionRepository;

    @Autowired
    private MinioClient minioClient;

    private final UUID courseId1 = UUID.randomUUID();
    private final UUID courseId2 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);
    private final LoggedInUser.CourseMembership courseMembership2 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId2);


    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMemberships(courseMembership1, courseMembership2);

    @Test
    void testCreateExerciseFile(final HttpGraphQlTester tester) {
        UUID assessmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        OffsetDateTime endDate = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(endDate);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity.setDownloadUrlExpiresAt(Instant.MIN);

        FileEntity exerciseFile = new FileEntity();
        exerciseFile.setName("TestFile");
        submissionExerciseEntity.getFiles().add(exerciseFile);

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setName("TestTask");
        taskEntity.setNumber(1);
        taskEntity.setMaxScore(100);
        taskEntity.setItemId(itemId);
        submissionExerciseEntity.getTasks().add(taskEntity);

        FileEntity solutionFile = new FileEntity();
        solutionFile.setName("TestSolution");

        FileEntity solutionFile2 = new FileEntity();
        solutionFile2.setName("TestSolution2");

        TaskResultEntity taskResultEntity = new TaskResultEntity();
        taskResultEntity.setItemId(itemId);
        taskResultEntity.setScore(100);
        taskResultEntity.setRequiredScore(100);
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setStatus(ResultEntity.Status.pending);
        resultEntity.setUserId(currentUser.getId());
        resultEntity.setResults(new ArrayList<>(List.of(taskResultEntity)));

        TaskResultEntity taskResultEntity2 = new TaskResultEntity();
        taskResultEntity2.setItemId(itemId);
        taskResultEntity2.setScore(100);
        taskResultEntity2.setRequiredScore(100);
        ResultEntity resultEntity2 = new ResultEntity();
        resultEntity2.setStatus(ResultEntity.Status.pending);
        resultEntity2.setUserId(user2Id);
        resultEntity2.setResults(new ArrayList<>(List.of(taskResultEntity2)));

        ExerciseSolutionEntity solutionEntity = new ExerciseSolutionEntity();
        solutionEntity.setUserId(currentUser.getId());
        solutionEntity.setSubmissionDate(OffsetDateTime.now());
        solutionEntity.setFiles(new ArrayList<>(List.of(solutionFile)));
        solutionEntity.setResult(resultEntity);

        ExerciseSolutionEntity solutionEntity2 = new ExerciseSolutionEntity();
        solutionEntity2.setUserId(user2Id);
        solutionEntity2.setSubmissionDate(OffsetDateTime.now());
        solutionEntity2.setFiles(new ArrayList<>(List.of(solutionFile2)));
        solutionEntity2.setResult(resultEntity2);


        submissionExerciseEntity.getSolutions().add(solutionEntity);
        submissionExerciseEntity.getSolutions().add(solutionEntity2);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                query {
                    submissionExerciseByUser(
                        assessmentId: "%s"
                        ) {
                        assessmentId,
                        courseId,
                        endDate,
                        solutions {
                            id,
                            userId,
                            submissionDate,
                            files {
                                id,
                                uploadUrl,
                                downloadUrl,
                                name
                            },
                            result {
                                id,
                                status,
                                results {
                                    itemId,
                                    score
                                }
                            }
                        }
                        tasks {
                            name,
                            itemId,
                            maxScore
                        },
                        files {
                            id,
                            uploadUrl,
                            downloadUrl,
                            name
                        }
                    }
                }
        """.formatted(submissionExerciseEntity.getAssessmentId());
        final SubmissionExercise submissionExercise = tester.document(query)
                .execute()
                .path("submissionExerciseByUser").entity(SubmissionExercise.class).get();
        Task task = submissionExercise.getTasks().get(0);
        SubmissionSolution solution = submissionExercise.getSolutions().get(0);
        assertThat(submissionExercise.getFiles().size(), is(1));
        assertThat(submissionExercise.getTasks().size(), is(1));
        assertThat(submissionExercise.getSolutions().size(), is(1));
        assertThat(submissionExercise.getFiles().get(0).getDownloadUrl(), is("http://example.com"));
        assertThat(submissionExercise.getCourseId(), is(courseId1));
        assertThat(task.getName(), is("TestTask"));
        assertThat(task.getMaxScore(), is(100));
        assertThat(solution.getUserId(), is(currentUser.getId()));
        assertThat(solution.getFiles().get(0).getDownloadUrl(), is("http://example.com"));
        assertThat(solution.getResult().getStatus(), is(Status.pending));
        assertThat(solution.getResult().getResults(), hasSize(1));
        assertThat(solution.getResult().getResults().get(0).getItemId(), is(itemId));
        assertThat(solution.getResult().getResults().get(0).getScore(), is(100));
    }
}
