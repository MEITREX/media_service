package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.service.SubmissionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourses;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final SubmissionExerciseRepository submissionExerciseRepository;

    private static final String SUBMISSION_MUTATION_NAME = "SubmissionMutation";

    @QueryMapping
    public SubmissionExercise submissionExerciseByUser(@Argument UUID assessmentId,
                                                       @ContextValue final LoggedInUser currentUser) {
        return submissionService.getSubmissionExerciseByUserId(assessmentId, currentUser.getId());
    }

    @QueryMapping
    public SubmissionExercise submissionExerciseForLecturer(@Argument UUID assessmentId,
                                                            @ContextValue final LoggedInUser currentUser) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + assessmentId + " not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, submissionExercise.getCourseId());
        return submissionService.getSubmissionExerciseForLecturer(assessmentId);
    }

    @MutationMapping(name = "_internal_noauth_createSubmissionExercise")
    public SubmissionExercise createSubmissionExercise(@Argument InputSubmissionExercise input,
                                                       @Argument UUID courseId,
                                                       @Argument UUID assessmentId) {
        return submissionService.createSubmissionExercise(input, assessmentId, courseId);
    }

    @MutationMapping
    public SubmissionSolution uploadSolution(@Argument InputSubmissionSolution solution,
                                             @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, solution.getCourseId());
        return submissionService.createSolution(currentUser.getId(), solution);
    }

    @MutationMapping
    public Result updateResult(@Argument InputResult result,
                               @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, result.getCourseId());
        return submissionService.updateResult(result);
    }

    @MutationMapping
    public File createExerciseFile(@Argument String name,
                                   @Argument UUID assessmentId,
                                   @ContextValue final LoggedInUser currentUser) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + assessmentId + " not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, submissionExercise.getCourseId());
        return submissionService.createFileForExercise(name, submissionExercise);
    }

    @MutationMapping
    public File createSolutionFile(@Argument String name,
                                   @Argument UUID solutionId,
                                   @Argument UUID assessmentId,
                                   @ContextValue final LoggedInUser currentUser) {
         return submissionService.createSolutionFile(currentUser.getId(), solutionId, assessmentId, name);
    }

    @MutationMapping
    public SubmissionMutation mutateSubmission(@Argument final UUID assessmentId,
                                               @ContextValue final LoggedInUser currentUser) {
        final SubmissionExerciseEntity submissionExercise = submissionService.requireSubmissionExerciseExists(assessmentId);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, submissionExercise.getCourseId());

        return new SubmissionMutation(assessmentId);
    }

    @SchemaMapping(typeName = SUBMISSION_MUTATION_NAME)
    public SubmissionExercise _internal_noauth_addTask(@Argument final InputTask input, final SubmissionMutation submissionMutation){
        return submissionService.addTask(submissionMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = SUBMISSION_MUTATION_NAME)
    public SubmissionExercise _internal_noauth_updateTask(@Argument final InputTask input, final SubmissionMutation submissionMutation){
        return submissionService.updateTask(submissionMutation.getAssessmentId(), input);
    }

    @SchemaMapping(typeName = SUBMISSION_MUTATION_NAME)
    public SubmissionExercise removeTask(@Argument final UUID itemId, final SubmissionMutation submissionMutation){
        return submissionService.deleteTask(submissionMutation.getAssessmentId(), itemId);
    }
}
