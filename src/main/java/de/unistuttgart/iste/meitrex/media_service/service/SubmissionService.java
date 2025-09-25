package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.*;
import de.unistuttgart.iste.meitrex.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.submission.*;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {
    public static final String BUCKET_ID = "submission-bucket";
    private static final Duration URL_EXPIRY = Duration.ofHours(6);       // how long you issue URLs for
    private static final Duration REFRESH_THRESHOLD = Duration.ofHours(2); // refresh when < 2h remain
    private static final Duration SKEW_BUFFER = Duration.ofMinutes(2);     // small safety margin


    private final SubmissionExerciseRepository submissionExerciseRepository;
    private final SubmissionFileRepository fileRepository;
    private final SubmissionExerciseSolutionRepository submissionExerciseSolutionRepository;
    private final SubmissionResultRepository resultRepository;
    private final ModelMapper modelMapper;

    private final TopicPublisher topicPublisher;

    private final MinioClient minioInternalClient;
    private final MinioClient minioExternalClient;

    public SubmissionExercise getSubmissionExerciseByUserId(UUID assessmentId, UUID userId) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + assessmentId + " not found"));
        updateSubmissionDownloadUrls(submissionExercise);
        List<ExerciseSolutionEntity> solution = submissionExercise.getSolutions().stream().filter(exerciseSolutionEntity -> exerciseSolutionEntity.getUserId().equals(userId)).toList();
        submissionExercise.setSolutions(solution);
        return modelMapper.map(submissionExercise, SubmissionExercise.class);
    }

    public SubmissionExercise getSubmissionExerciseForLecturer(UUID assessmentId) {
        SubmissionExerciseEntity submissionExercise = submissionExerciseRepository.findById(assessmentId).orElseThrow(() ->
                new EntityNotFoundException("Exercise with id: " + assessmentId + " not found"));
        updateSubmissionDownloadUrls(submissionExercise);
        return modelMapper.map(submissionExercise, SubmissionExercise.class);
    }

    private void updateSubmissionDownloadUrls(SubmissionExerciseEntity submissionExercise) {
        AtomicBoolean changed = new AtomicBoolean(false);
        submissionExercise.getFiles().forEach(submissionExerciseEntity -> {
            if(isExpiringSoon(submissionExerciseEntity.getDownloadUrlExpiresAt())) {
                createDownloadUrl(submissionExerciseEntity);
                changed.set(true);
            }
        });
        submissionExercise.getSolutions().forEach(exerciseSolutionEntity ->
                exerciseSolutionEntity.getFiles().forEach(fileEntity -> {
                    if(isExpiringSoon(fileEntity.getDownloadUrlExpiresAt())) {
                        createDownloadUrl(fileEntity);
                        changed.set(true);
                    }
                }));
        if (changed.get()) {
            submissionExerciseRepository.save(submissionExercise);
        }
    }

    public SubmissionExercise createSubmissionExercise(InputSubmissionExercise submissionExercise, UUID assessmentId, UUID courseId) {
        SubmissionExerciseEntity submissionExerciseEntity = new SubmissionExerciseEntity();
        submissionExerciseEntity.setAssessmentId(assessmentId);
        submissionExerciseEntity.setCourseId(courseId);
        submissionExerciseEntity.setEndDate(submissionExercise.getEndDate());
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
        exerciseSolutionEntity.setResult(initialResultEntity(userId, submissionExercise.getTasks()));
        submissionExercise.getSolutions().add(exerciseSolutionEntity);
        submissionExerciseRepository.save(submissionExercise);
        return  modelMapper.map(solution, SubmissionSolution.class);
    }

    public Result updateResult(InputResult result) {
        ResultEntity resultEntity = resultRepository.findById(result.getId()).orElseThrow(() ->
                new  EntityNotFoundException("Result with id: " + result.getId() + " not found"));
        resultEntity.setStatus(modelMapper.map(result.getStatus(), ResultEntity.Status.class));
        result.getResults().forEach(taskResult -> resultEntity.getResults().stream()
                .filter(taskResultEntity -> taskResultEntity.getTaskId()
                .equals(taskResult.getTaskId())).findFirst()
                .ifPresent( taskResultEntity -> taskResultEntity.setScore(taskResult.getScore())));
        List<Response> responses = new ArrayList<>();
        AtomicInteger requiredScore = new AtomicInteger();
        AtomicInteger achievedScore = new AtomicInteger();
        resultEntity.getResults().forEach(taskResult -> {
            requiredScore.addAndGet(taskResult.getRequiredScore());
            achievedScore.addAndGet(taskResult.getScore());
            Response response = new Response(taskResult.getTaskId(), (float) taskResult.getRequiredScore() / taskResult.getScore());
            responses.add(response);
        });

        double correctness = (double)requiredScore.get() / (double)achievedScore.get();

        final ContentProgressedEvent userProgressLogEvent = ContentProgressedEvent.builder()
                .userId(resultEntity.getUserId())
                .contentId(result.getAssessmentId())
                .hintsUsed(0)
                .success(resultEntity.getStatus().equals(ResultEntity.Status.passed))
                .timeToComplete(null)
                .contentType(ContentProgressedEvent.ContentType.OTHER)
                .correctness(correctness)
                .responses(responses)
                .build();

        // publish new user progress event message
        topicPublisher.notifyUserWorkedOnContent(userProgressLogEvent);

        resultRepository.save(resultEntity);
        return modelMapper.map(resultEntity, Result.class);
    }

    public File createFileForExercise(String filename, SubmissionExerciseEntity submissionExercise) {
        FileEntity fileEntity = createFile(filename);
        createUploadUrl(fileEntity);
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
        createUploadUrl(fileEntity);
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

    @Scheduled(cron = "0 0 * * * *", zone = "Europe/Berlin") // every hour
    public void expireUploadUrlsAndCleanupPlaceholders() {
        Instant now = Instant.now();

        List<FileEntity> files = fileRepository.findCandidates();

        for (FileEntity f : files) {
            boolean changed = false;
            String objectName = f.getId().toString();
            boolean exists = safeDoesObjectExist(objectName);

            // Null expired upload URLs
            if (f.getUploadUrl() != null && f.getUploadUrlExpiresAt() != null
                    && now.isAfter(f.getUploadUrlExpiresAt())) {
                f.setUploadUrl(null);
                f.setUploadUrlExpiresAt(null);
                changed = true;
            }

            if (!exists && f.getUploadUrlExpiresAt() != null
                    && now.isAfter(f.getUploadUrlExpiresAt())) { // optionally .plus(DELETE_GRACE)
                removeFileEntityFromParentsAndDelete(f);
                continue; // entity gone
            }

            if (changed) {
                fileRepository.save(f);
            }
        }
    }

    private void removeFileEntityFromParentsAndDelete(FileEntity f) {
        // Option A: If you have orphanRemoval = true on @OneToMany relations to files,
        // just remove it from the owning collections and save the owners.
        // submissionExercise.getFiles().remove(f), etc.

        // Example (pseudo—adjust to your mappings):
        submissionExerciseRepository.findAll().forEach(se -> {
            if (se.getFiles().removeIf(x -> x.getId().equals(f.getId()))) {
                submissionExerciseRepository.save(se);
            }
            se.getSolutions().forEach(sol -> {
                if (sol.getFiles().removeIf(x -> x.getId().equals(f.getId()))) {
                    submissionExerciseSolutionRepository.save(sol);
                }
            });
        });

        // Then delete the file row
        fileRepository.delete(f);
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

    private boolean safeDoesObjectExist(String name) {
        try {
            minioInternalClient.statObject(StatObjectArgs.builder()
                    .bucket(BUCKET_ID)
                    .object(name)
                    .build());
            return true;
        } catch (io.minio.errors.ErrorResponseException e) {
            // typical not-found (NoSuchKey) → return false
            if ("NoSuchKey".equalsIgnoreCase(e.errorResponse().code())) return false;
            log.warn("MinIO error while statObject (treating as not exists): {}", e.errorResponse().code());
            return false; // fail closed (don’t expose stale URLs)
        } catch (Exception e) {
            log.warn("MinIO statObject failed, treating as not exists", e);
            return false;
        }
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

    private FileEntity createFile(String name) {
        FileEntity fileEntity = new FileEntity();
        fileEntity.setName(name);
        fileRepository.saveAndFlush(fileEntity);
        return fileEntity;
    }

    @SneakyThrows
    private void createUploadUrl(FileEntity fileEntity) {
        final String uploadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(BUCKET_ID)
                        .object(fileEntity.getId().toString())
                        .expiry(URL_EXPIRY.toHoursPart(), TimeUnit.HOURS)
                        .build());
        fileEntity.setUploadUrl(uploadUrl);
        fileEntity.setUploadUrlExpiresAt(Instant.now().plus(URL_EXPIRY));
        fileRepository.save(fileEntity);
    }

    @SneakyThrows
    private void createDownloadUrl(FileEntity fileEntity) {
        final String downloadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(BUCKET_ID)
                        .object(fileEntity.getId().toString())
                        .expiry(URL_EXPIRY.toHoursPart(), TimeUnit.HOURS)
                        .build());
        fileEntity.setDownloadUrl(downloadUrl);
        fileEntity.setDownloadUrlExpiresAt(Instant.now().plus(URL_EXPIRY));
        fileRepository.save(fileEntity);
    }

    private boolean isExpiringSoon(Instant expiresAt) {
        if (expiresAt == null) return true; // no info → refresh
        Instant now = Instant.now();
        return now.plus(REFRESH_THRESHOLD).plus(SKEW_BUFFER).isAfter(expiresAt);
    }
}
