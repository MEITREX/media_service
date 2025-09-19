package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.event.CourseChangeEvent;
import de.unistuttgart.iste.meitrex.common.event.CrudOperation;
import de.unistuttgart.iste.meitrex.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.ExerciseSolutionEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.FileEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.ResultEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.SubmissionExerciseEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionExerciseSolutionRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionFileRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.SubmissionResultRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {
    public static final String BUCKET_ID = "submission-bucket";

    private final SubmissionExerciseRepository submissionExerciseRepository;
    private final SubmissionFileRepository fileRepository;
    private final SubmissionExerciseSolutionRepository submissionExerciseSolutionRepository;
    private final SubmissionResultRepository resultRepository;
    private final ModelMapper modelMapper;

    private final MinioClient minioInternalClient;
    private final MinioClient minioExternalClient;

    public SubmissionExercise getSubmissionExerciseByUserId(UUID exerciseId, UUID userId) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(exerciseId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + exerciseId + " not found"));
        updateSubmissionUrls(submissionExercise);
        submissionExerciseRepository.save(submissionExercise);
        List<ExerciseSolutionEntity> solution = submissionExercise.getSolutions().stream().filter(exerciseSolutionEntity -> exerciseSolutionEntity.getUserId().equals(userId)).toList();
        submissionExercise.setSolutions(solution);
        return modelMapper.map(submissionExercise, SubmissionExercise.class);
    }

    public SubmissionExercise getSubmissionExerciseForLecturer(UUID exerciseId) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(exerciseId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + exerciseId + " not found"));
        updateSubmissionUrls(submissionExercise);
        submissionExerciseRepository.save(submissionExercise);
        return modelMapper.map(submissionExercise, SubmissionExercise.class);
    }

    private void updateSubmissionUrls(SubmissionExerciseEntity submissionExercise) {
        submissionExercise.getFiles().forEach(this::createUploadAndDownloadUrl);
        submissionExercise.getSolutions().forEach(exerciseSolutionEntity ->
        {exerciseSolutionEntity.getFiles().forEach(this::createUploadAndDownloadUrl);});
    }

    public SubmissionExercise createSubmissionExercise(InputSubmissionExercise submissionExercise) {
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setCourseId(submissionExercise.getCourseId());
        submissionExerciseEntity.setStartTime(submissionExercise.getStartTime());
        submissionExerciseEntity.setEndTime(submissionExercise.getEndTime());
        submissionExerciseEntity.setMaxScore(submissionExercise.getMaxScore());
        submissionExerciseEntity.setFiles(new ArrayList<>());
        submissionExerciseEntity.setSolutions(new ArrayList<>());
        submissionExerciseEntity.setTasks(new ArrayList<>());
        submissionExerciseRepository.save(submissionExerciseEntity);
        return modelMapper.map(submissionExerciseEntity, SubmissionExercise.class);
    }

    public SubmissionSolution createSolution(UUID userId, InputSubmissionSolution solution) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(solution.getSubmissionExerciseId()).orElseThrow(()
                -> new  EntityNotFoundException("Solution with id: " + solution.getSubmissionExerciseId() + " not found"));
        ExerciseSolutionEntity exerciseSolutionEntity = new ExerciseSolutionEntity();
        exerciseSolutionEntity.setUserId(userId);
        exerciseSolutionEntity.setFiles(new ArrayList<>());
        exerciseSolutionEntity.setResult(initialResultEntity());
        submissionExercise.getSolutions().add(exerciseSolutionEntity);
        submissionExerciseRepository.save(submissionExercise);
        return  modelMapper.map(solution, SubmissionSolution.class);
    }

    public Result updateResult(InputResult result) {
        ResultEntity resultEntity = resultRepository.findById(result.getId()).orElseThrow(() ->
                new  EntityNotFoundException("Result with id: " + result.getId() + " not found"));
        resultEntity.setScore(result.getScore());
        resultEntity.setStatus(modelMapper.map(result.getStatus(), ResultEntity.Status.class));
        resultRepository.save(resultEntity);
        return modelMapper.map(resultEntity, Result.class);
    }

    public File createFileForExercise(String filename, SubmissionExerciseEntity submissionExercise) {
        FileEntity fileEntity = createFile(filename);
        createUploadAndDownloadUrl(fileEntity);
        submissionExercise.getFiles().add(fileEntity);
        submissionExerciseRepository.save(submissionExercise);
        return modelMapper.map(fileEntity, File.class);
    }

    public File createSolutionFile(UUID userId, UUID solutionId, String filename) {
        ExerciseSolutionEntity exerciseSolutionEntity = submissionExerciseSolutionRepository.findById(solutionId).orElseThrow(() ->
                new  EntityNotFoundException("Solution with id: " + solutionId + " not found"));
        if (!userId.equals(exerciseSolutionEntity.getUserId())) {
            throw new EntityNotFoundException("Solution with id: " + solutionId + " does not belong to the user");
        }
        FileEntity fileEntity = createFile(filename);
        createUploadAndDownloadUrl(fileEntity);
        exerciseSolutionEntity.getFiles().add(fileEntity);
        submissionExerciseSolutionRepository.save(exerciseSolutionEntity);
        return modelMapper.map(fileEntity, File.class);
    }

    /**
     * Method that receives Course Change Event and handles DELETE events.
     * All submission exercises are then deleted that are connected to deleted course
     *
     * @param changeEvent a Course Change Event received over dapr
     * @throws IncompleteEventMessageException if the received message is incomplete
     */
    public void deleteCourse(final CourseChangeEvent changeEvent) throws IncompleteEventMessageException{
        // evaluate course Update message
        if (changeEvent.getCourseId() == null || changeEvent.getOperation() == null) {
            throw new IncompleteEventMessageException("Incomplete message received: all fields of a message must be non-null");
        }
        // only consider DELETE events
        if (changeEvent.getOperation() != CrudOperation.DELETE) {
            return;
        }

        List<SubmissionExerciseEntity> submissionExerciseEntities = submissionExerciseRepository.findAllByCourseId(changeEvent.getCourseId());
        submissionExerciseEntities.forEach(submissionExerciseEntity -> {
            submissionExerciseEntity.getFiles().forEach(this::deleteFile);
            submissionExerciseEntity.getSolutions().forEach(exerciseSolutionEntity ->
                    exerciseSolutionEntity.getFiles().forEach(this::deleteFile));
        });
        submissionExerciseRepository.deleteAll(submissionExerciseEntities);
    }

    @SneakyThrows
    private void deleteFile(final FileEntity fileEntity) {
        String filename = fileEntity.getId().toString();

        if (doesObjectExist(filename)) {
            minioInternalClient.removeObject(
                    RemoveObjectArgs
                            .builder()
                            .bucket(BUCKET_ID)
                            .object(filename)
                            .build());
        }
    }

    private boolean doesObjectExist(final String name) {
        try {
            minioInternalClient.statObject(StatObjectArgs.builder()
                    .bucket(SubmissionService.BUCKET_ID)
                    .object(name).build());
            return true;
        } catch (final ErrorResponseException e) {
            log.error("Object not found", e);
            return false;
        } catch (final Exception e) {
            log.error("Error while checking if object exists", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private ResultEntity initialResultEntity() {
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setStatus(ResultEntity.Status.pending);
        resultEntity.setScore(0);
        return resultEntity;
    }

    private FileEntity createFile(String name) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(name + "_submission");
        fileRepository.saveAndFlush(fileEntity);
        return fileEntity;
    }


    @SneakyThrows
    private void createUploadAndDownloadUrl(FileEntity fileEntity) {
        final String uploadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(BUCKET_ID)
                        .object(fileEntity.getId().toString())
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
        fileEntity.setUploadUrl(uploadUrl);
        final String downloadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(BUCKET_ID)
                        .object(fileEntity.getId().toString())
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
        fileEntity.setDownloadUrl(downloadUrl);
        fileRepository.save(fileEntity);
    }
}
