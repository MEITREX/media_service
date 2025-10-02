package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.SubmissionMutation;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.TaskEntity;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@GraphQlApiTest
public class MutationMutateSubmissionTest {

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
    void testMutateSubmissionAddTask(final HttpGraphQlTester tester) {
        UUID assessmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(createdAt);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);


        final String query = """
                mutation {
                    mutateSubmission(
                        assessmentId: "%s"
                        ) {
                        assessmentId,
                        _internal_noauth_addTask(input: {
                            number: 1,
                            name: "TestTaskName",
                            itemId: "%s",
                            maxScore: 100
                        }) {
                            assessmentId
                            courseId
                            endDate
                            tasks {
                                name,
                                itemId,
                                maxScore,
                                number
                            }
                        }
                    }
                }
        """.formatted(submissionExerciseEntity.getAssessmentId(), itemId);
        final SubmissionMutation submissionMutation = tester.document(query)
                .execute()
                .path("mutateSubmission").entity(SubmissionMutation.class).get();
        assertThat(submissionMutation.getAssessmentId(), is(assessmentId));
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).get();
        TaskEntity task = submissionExercise.getTasks().stream().findFirst().get();
        assertThat(task.getName(), is("TestTaskName"));
        assertThat(task.getItemId(), is(itemId));
        assertThat(task.getMaxScore(), is(100));
        assertThat(task.getNumber(), is(1));
    }

    @Test
    void testMutateSubmissionMutateTask(final HttpGraphQlTester tester) {
        UUID assessmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(createdAt);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);

        TaskEntity task = new TaskEntity();
        task.setName("TestTaskName");
        task.setItemId(itemId);
        task.setMaxScore(100);
        task.setNumber(1);
        submissionExerciseEntity.getTasks().add(task);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                mutation {
                    mutateSubmission(
                        assessmentId: "%s"
                        ) {
                        assessmentId,
                        _internal_noauth_updateTask(input: {
                            number: 2,
                            name: "TestTaskNameUpdate",
                            itemId: "%s",
                            maxScore: 200
                        }) {
                            assessmentId
                            courseId
                            endDate
                            tasks {
                                name,
                                itemId,
                                maxScore
                            }
                        }
                    }
                }
        """.formatted(submissionExerciseEntity.getAssessmentId(), itemId);
        final SubmissionMutation submissionMutation = tester.document(query)
                .execute()
                .path("mutateSubmission").entity(SubmissionMutation.class).get();
        assertThat(submissionMutation.getAssessmentId(), is(assessmentId));
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).get();
        TaskEntity updatedTask = submissionExercise.getTasks().stream().findFirst().get();
        assertThat(updatedTask.getName(), is("TestTaskNameUpdate"));
        assertThat(updatedTask.getItemId(), is(itemId));
        assertThat(updatedTask.getMaxScore(), is(200));
        assertThat(updatedTask.getNumber(), is(2));
    }

    @Test
    void testMutateSubmissionDelete(final HttpGraphQlTester tester) {
        UUID assessmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(createdAt);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);

        TaskEntity task = new TaskEntity();
        task.setName("TestTaskName");
        task.setItemId(itemId);
        task.setMaxScore(100);
        task.setNumber(1);
        submissionExerciseEntity.getTasks().add(task);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                mutation {
                    mutateSubmission(
                        assessmentId: "%s"
                        ) {
                        assessmentId,
                        removeTask(itemId: "%s") {
                            assessmentId
                        }
                    }
                }
        """.formatted(submissionExerciseEntity.getAssessmentId(), itemId);
        final SubmissionMutation submissionMutation = tester.document(query)
                .execute()
                .path("mutateSubmission").entity(SubmissionMutation.class).get();
        assertThat(submissionMutation.getAssessmentId(), is(assessmentId));
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).get();
        assertThat(submissionExercise.getTasks().size(), is(0));
    }

    @Test
    void testMutateSubmissionMutateSubmission(final HttpGraphQlTester tester) {
        UUID assessmentId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        OffsetDateTime endDate = OffsetDateTime.now();
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(endDate);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);

        TaskEntity task = new TaskEntity();
        task.setName("TestTaskName");
        task.setItemId(itemId);
        task.setMaxScore(100);
        task.setNumber(1);
        submissionExerciseEntity.getTasks().add(task);
        submissionExerciseRepository.save(submissionExerciseEntity);

        OffsetDateTime newEndDate = OffsetDateTime.now().plusDays(2);

        final String query = """
                mutation {
                    mutateSubmission(
                        assessmentId: "%s"
                        ) {
                        assessmentId,
                        mutateSubmission(assessmentId: "%s", input: {
                            endDate: "%s"
                        }) {
                            assessmentId
                        }
                    }
                }
        """.formatted(submissionExerciseEntity.getAssessmentId(), submissionExerciseEntity.getAssessmentId(), newEndDate);
        final SubmissionMutation submissionMutation = tester.document(query)
                .execute()
                .path("mutateSubmission").entity(SubmissionMutation.class).get();
        assertThat(submissionMutation.getAssessmentId(), is(assessmentId));
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).get();
        assertThat(submissionExercise.getEndDate(), is(newEndDate));
    }
}
