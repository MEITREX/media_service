package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.File;
import de.unistuttgart.iste.meitrex.generated.dto.Result;
import de.unistuttgart.iste.meitrex.generated.dto.Status;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionFileRepository;
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
public class MutationDeleteExerciseFileTest {
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
    @Autowired
    private SubmissionFileRepository submissionFileRepository;

    @Test
    void testDeleteExerciseFile(final HttpGraphQlTester tester)throws  Exception {
        UUID assessmentId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        FileEntity fileEntity = new FileEntity();
        fileEntity.setName("file1");
        fileEntity.setUploadUrl("example.com");
        fileEntity.setDownloadUrl("example.com");
        fileEntity = submissionFileRepository.save(fileEntity);

        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(createdAt);
        submissionExerciseEntity.setFiles(new ArrayList<>(List.of(fileEntity)));
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
        exerciseSolutionRepository.save(exerciseSolutionEntity);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                mutation {
                    deleteExerciseFile(
                        fileId: "%s",
                        assessmentId: "%s",
                        ) {
                        id,
                        uploadUrl,
                        downloadUrl,
                        name
                    }
                }
        """.formatted(fileEntity.getId(), assessmentId);
        final File file = tester.document(query)
                .execute()
                .path("deleteExerciseFile").entity(File.class).get();
        assertThat(file.getId(), is(fileEntity.getId()));
        assertThat(submissionFileRepository.findById(fileEntity.getId()).isEmpty(), is(true));
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
