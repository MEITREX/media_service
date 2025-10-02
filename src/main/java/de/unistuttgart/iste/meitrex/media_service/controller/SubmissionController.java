package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.ExerciseSolutionEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.service.SubmissionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final SubmissionExerciseRepository submissionExerciseRepository;

    private static final String SUBMISSION_MUTATION_NAME = "SubmissionMutation";
    private final ExerciseSolutionRepository exerciseSolutionRepository;

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
        return submissionService.createSolution(currentUser.getId(), currentUser.getUserName(), solution);
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
    public File deleteExerciseFile(@Argument UUID fileId,
                                   @Argument UUID assessmentId,
                                   @ContextValue final LoggedInUser currentUser) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).orElseThrow(() ->
                new  EntityNotFoundException("Exercise with id: " + assessmentId + " not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, submissionExercise.getCourseId());
        return submissionService.deleteExerciseFile(submissionExercise, fileId);
    }

    @MutationMapping
    public File createSolutionFile(@Argument String name,
                                   @Argument UUID solutionId,
                                   @Argument UUID assessmentId,
                                   @ContextValue final LoggedInUser currentUser) {
         return submissionService.createSolutionFile(currentUser.getId(), solutionId, assessmentId, name);
    }

    @MutationMapping
    public File deleteSolutionFile(@Argument UUID fileId,
                                   @Argument UUID solutionId,
                                   @ContextValue final LoggedInUser currentUser) {
        ExerciseSolutionEntity exerciseSolutionEntity = exerciseSolutionRepository.findById(solutionId)
                .orElseThrow(() -> new EntityNotFoundException("Solution with id: " + solutionId + " not found"));
        if (!exerciseSolutionEntity.getUserId().equals(currentUser.getId())) {
            throw new EntityNotFoundException("User with id " + currentUser.getId() + " did not match the Id of of the uploader.");
        }
        return submissionService.deleteSolutionFile(exerciseSolutionEntity, fileId);
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

    @SchemaMapping(typeName = SUBMISSION_MUTATION_NAME)
    public SubmissionExercise mutateSubmission(@Argument final UUID assessmentId, @Argument final InputSubmissionExercise input){
        return submissionService.mutateSubmissionExercise(assessmentId, input);
    }
}
