package de.unistuttgart.iste.meitrex.media_service.api.submission;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.File;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.ExerciseSolutionEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.test_config.MockMinIoClientConfiguration;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMemberships;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ContextConfiguration(classes = MockMinIoClientConfiguration.class)
@Transactional
@GraphQlApiTest
public class MutationCreateSolutionFileTest {
    @Autowired
    private SubmissionExerciseRepository submissionExerciseRepository;

    @Autowired
    private SubmissionExerciseSolutionRepository submissionExerciseSolutionRepository;

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
        String fileName = "TestFilename";
        OffsetDateTime endDate = OffsetDateTime.now().plusDays(2);
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId1);
        submissionExerciseEntity.setEndDate(endDate);
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseEntity = submissionExerciseRepository.save(submissionExerciseEntity);

        ExerciseSolutionEntity solutionEntity = new ExerciseSolutionEntity();
        solutionEntity.setUserId(currentUser.getId());
        solutionEntity.setFiles(new ArrayList<>());

        submissionExerciseEntity.getSolutions().add(solutionEntity);
        solutionEntity = submissionExerciseSolutionRepository.save(solutionEntity);
        submissionExerciseRepository.save(submissionExerciseEntity);

        final String query = """
                mutation {
                    createSolutionFile(
                        name: "%s",
                        solutionId: "%s",
                        assessmentId: "%s"
                        ) {
                        id,
                        uploadUrl,
                        downloadUrl,
                        name
                    }
                }
        """.formatted(fileName, solutionEntity.getId(), assessmentId);
        final File file = tester.document(query)
                .execute()
                .path("createSolutionFile").entity(File.class).get();
        assertThat(file.getName(), is(fileName));
        assertThat(file.getUploadUrl(), is("http://example.com") );
        assertThat(file.getDownloadUrl(), nullValue());
        ExerciseSolutionEntity exerciseSolution = submissionExerciseSolutionRepository.findById(solutionEntity.getId()).get();
        assertThat((double) exerciseSolution.getSubmissionDate().toInstant().toEpochMilli(),
                closeTo((double) OffsetDateTime.now().toInstant().toEpochMilli(), 100000.0));
    }
}
