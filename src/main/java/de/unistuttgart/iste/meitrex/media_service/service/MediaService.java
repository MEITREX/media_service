package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ContentChangeEvent;
import de.unistuttgart.iste.meitrex.common.event.CrudOperation;
import de.unistuttgart.iste.meitrex.common.event.MediaRecordDeletedEvent;
import de.unistuttgart.iste.meitrex.common.event.MediaRecordFileCreatedEvent;
import de.unistuttgart.iste.meitrex.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.meitrex.generated.dto.CreateMediaRecordInput;
import de.unistuttgart.iste.meitrex.generated.dto.MediaRecord;
import de.unistuttgart.iste.meitrex.generated.dto.UpdateMediaRecordInput;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class which provides the business logic of the media service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MediaService {

    public static final String BUCKET_ID = "bucketId";
    public static final String FILENAME = "filename";
    public static final String MEDIA_RECORDS_NOT_FOUND = "Media record(s) with id(s) %s not found.";

    private final MinioClient minioInternalClient;
    private final MinioClient minioExternalClient;

    private final TopicPublisher topicPublisher;

    /**
     * Database repository storing our media records.
     */
    private final MediaRecordRepository repository;
    /**
     * Mapper used to map media record DTOs to database Entities and vice-versa.
     */
    private final ModelMapper modelMapper;

    /**
     * Returns all media records.
     *
     * @return Returns a list containing all saved media records.
     */
    public List<MediaRecord> getAllMediaRecords() {
        final List<MediaRecord> records = repository.findAll().stream()
                .map(this::mapEntityToMediaRecord)
                .toList();

        removeExpiredUrlsFromMediaRecords(records);

        return records;
    }

    /**
     * When passed a list of media record ids, returns a list containing the records matching these ids, or throws
     * an EntityNotFoundException when there is no matching record for one or more of the passed ids.
     *
     * @param ids                  The ids to retrieve the records for.
     * @return List of the records with matching ids.
     * @throws EntityNotFoundException Thrown when one or more passed ids do not have corresponding media records in
     *                                 the database.
     */
    public List<MediaRecord> getMediaRecordsByIds(final List<UUID> ids) {
        final List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

        checkForMissingMediaRecords(ids, records);

        return records.stream()
                .map(x -> removeExpiredUrlsFromMediaRecord(modelMapper.map(x, MediaRecord.class)))
                .toList();
    }

    /**
     * The same as {@link #getMediaRecordsByIds(List)}, except that it doesn't throw an exception
     * if an entity cannot be found. Instead, it returns NULL for that entity.
     *
     * @return Returns a List containing the MediaRecords with the specified ids. If a media record for an id cannot
     * be found, returns NULL for that media record instead.
     */
    public List<MediaRecord> findMediaRecordsByIds(final List<UUID> ids) {
        final List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

        final List<MediaRecord> result = new ArrayList<>(ids.size());

        // go over all requested ids
        for (final UUID id : ids) {
            // get the entity with the matching id or NULL if it doesn't exist
            final MediaRecordEntity entity = records.stream().filter(x -> x.getId().equals(id)).findAny().orElse(null);
            MediaRecord mediaRecord = null;
            // if we found an entity, convert it to a DTO
            if (entity != null) {
                mediaRecord = modelMapper.map(entity, MediaRecord.class);
            }
            result.add(mediaRecord);
        }

        removeExpiredUrlsFromMediaRecords(result);

        return result;
    }

    public MediaRecord getMediaRecordById(final UUID id) {
        return removeExpiredUrlsFromMediaRecord(mapEntityToMediaRecord(requireMediaRecordExisting(id)));
    }

    /**
     * Gets all media records for which the specified user is the creator.
     *
     * @param userId The id of the user to get the media records for.
     * @return Returns a list of the user's media records.
     */
    public List<MediaRecord> getMediaRecordsForUser(final UUID userId) {
        final List<MediaRecordEntity> records = repository.findMediaRecordEntitiesByCreatorId(userId);

        return removeExpiredUrlsFromMediaRecords(
                records.stream().map(x -> modelMapper.map(x, MediaRecord.class)).toList());
    }

    /**
     * Gets all media records for which the specified users are the creator.
     *
     * @param userIds list of userIds to get the media records for.
     * @return a list of lists of media records for each user.
     */
    public List<List<MediaRecord>> getMediaRecordsForUsers(final List<UUID> userIds) {
        final List<List<MediaRecord>> result = new ArrayList<>();
        for (final UUID userId: userIds) {
            List<MediaRecord> userMediaRecords = repository.findMediaRecordEntitiesByCreatorId(userId).stream()
                            .map(x -> modelMapper.map(x, MediaRecord.class)).toList();

            removeExpiredUrlsFromMediaRecords(userMediaRecords);

            result.add(userMediaRecords);
        }

        return result;
    }

    /**
     * Gets all media records that are associated with the passed content ids.
     *
     * @param contentIds           The content ids to get the media records for.
     * @return Returns a list of lists, where each sublist stores the media records that are associated with the content
     * id at the same index in the passed list.
     */
    public List<List<MediaRecord>> getMediaRecordsByContentIds(final List<UUID> contentIds) {
        final List<MediaRecordEntity> records = repository.findMediaRecordEntitiesByContentIds(contentIds);

        // create our resulting list
        final List<List<MediaRecord>> result = new ArrayList<>(contentIds.size());

        // fill it with empty lists for each content id so that we can later fill them with
        // the media records associated with that content id
        for (int i = 0; i < contentIds.size(); i++) {
            result.add(new ArrayList<>());
        }

        // loop over all the entities we got and put them in their respective lists
        for (final MediaRecordEntity entity : records) {
            for (int i = 0; i < contentIds.size(); i++) {
                final UUID contentId = contentIds.get(i);
                if (entity.getContentIds().contains(contentId)) {
                    result.get(i).add(removeExpiredUrlsFromMediaRecord(mapEntityToMediaRecord(entity)));
                }
            }
        }

        return result;
    }

    /**
     * Gets all media records that are associated with the passed content id.
     *
     * @param contentId The content id to get the media records for.
     * @return Returns a list of media records that are associated with the content id.
     */
    public List<MediaRecordEntity> getMediaRecordEntitiesByContentId(final UUID contentId) {
        return repository.findMediaRecordEntitiesByContentIds(List.of(contentId));
    }

    /**
     * Gets all media records that are associated with the passed course id.
     *
     * @param courseIds The course id to get the media records for.
     * @return Returns a list of media records that are associated with the course id.
     */
    public List<List<MediaRecord>> getMediaRecordsForCourses(final List<UUID> courseIds) {
        final List<MediaRecordEntity> records = repository.findMediaRecordEntitiesByCourseIds(courseIds);

        // create our resulting list
        final List<List<MediaRecord>> result = new ArrayList<>(courseIds.size());

        // fill it with empty lists for each content id so that we can later fill them with
        // the media records associated with that content id
        for (int i = 0; i < courseIds.size(); i++) {
            result.add(new ArrayList<>());
        }

        // loop over all the entities we got and put them in their respective lists
        for (final MediaRecordEntity entity : records) {
            for (int i = 0; i < courseIds.size(); i++) {
                final UUID courseId = courseIds.get(i);
                if (entity.getCourseIds().contains(courseId)) {
                    result.get(i).add(removeExpiredUrlsFromMediaRecord(mapEntityToMediaRecord(entity)));
                }
            }
        }

        return result;
    }

    /**
     * Finds a media record with the specified id or throws an EntityNotFoundException if no such media record exists.
     *
     * @param id The id of the media record to find.
     * @return Returns the media record with the specified id.
     * @throws EntityNotFoundException Thrown when no media record with the specified id exists.
     */
    protected MediaRecordEntity requireMediaRecordExisting(final UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media record with id " + id + " not found."));
    }

    /**
     * Sets the linked media records of a given content to the media records specified by the passed ids.
     *
     * @param contentId      The content id of which the linked media records should be set.
     * @param mediaRecordIds The ids of the media records to link to the content.
     * @return Returns a list of the media records that were linked to the content.
     */
    public List<MediaRecord> setLinkedMediaRecordsForContent(final UUID contentId, final List<UUID> mediaRecordIds) {
        final List<MediaRecordEntity> mediaRecordsCurrentlyLinkedToContent
                = repository.findMediaRecordEntitiesByContentIds(List.of(contentId));

        // remove contentId from all media records that are currently linked to it
        for (final MediaRecordEntity entity : mediaRecordsCurrentlyLinkedToContent) {
            entity.getContentIds().remove(contentId);
            repository.save(entity);
        }

        final List<MediaRecordEntity> mediaRecordsToBeLinkedToContent = repository.findAllById(mediaRecordIds);

        checkForMissingMediaRecords(mediaRecordIds, mediaRecordsToBeLinkedToContent);

        for (final MediaRecordEntity entity : mediaRecordsToBeLinkedToContent) {
            entity.getContentIds().add(contentId);
            repository.save(entity);
        }

        return mediaRecordsToBeLinkedToContent.stream()
                .map(x -> removeExpiredUrlsFromMediaRecord(modelMapper.map(x, MediaRecord.class)))
                .toList();
    }

    /**
     * Sets the course of the selected mediaRecords.
     *
     * @param courseId      The course id of which the media records should be added to.
     * @param mediaRecordIds The ids of the media records to be added to the course.
     * @return Returns a list of the media records that were added to the course.
     */
    public List<MediaRecord> setMediaRecordsForCourse(final UUID courseId, final List<UUID> mediaRecordIds) {
        final List<MediaRecordEntity> currentMediaRecords
                = repository.findAllById(mediaRecordIds);

        // remove courseId from all media records which currently contain it
        for (final MediaRecordEntity entity : currentMediaRecords) {
            entity.getCourseIds().remove(courseId);
            repository.save(entity);
        }

        final List<MediaRecordEntity> mediaRecordsToAddCourse = repository.findAllById(mediaRecordIds);

        checkForMissingMediaRecords(mediaRecordIds, mediaRecordsToAddCourse);

        for (final MediaRecordEntity entity : mediaRecordsToAddCourse) {
            entity.getCourseIds().add(courseId);
            repository.save(entity);
        }

        return removeExpiredUrlsFromMediaRecords(mediaRecordsToAddCourse.stream()
                .map(x -> modelMapper.map(x, MediaRecord.class))
                .toList());
    }

    /**
     * if there are fewer returned records than passed ids, that means that some ids could not be found in the
     * db. In that case, calculate the difference of the two lists and throw an exception listing which ids
     * could not be found
     *
     * @param mediaRecordIds ids that should be checked
     * @param entities       entities that were found
     * @throws EntityNotFoundException if there are fewer returned records than passed ids
     */
    private void checkForMissingMediaRecords(final List<UUID> mediaRecordIds, final List<MediaRecordEntity> entities) {
        if (entities.size() != mediaRecordIds.size()) {
            final List<UUID> missingIds = new ArrayList<>(mediaRecordIds);
            missingIds.removeAll(entities.stream().map(MediaRecordEntity::getId).toList());

            throw new EntityNotFoundException(MEDIA_RECORDS_NOT_FOUND
                    .formatted(missingIds.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        }
    }

    /**
     * Creates a new media record with the attributes specified in the input argument.
     *
     * @param input               Object storing the attributes the newly created media record should have.
     * @param creatorId           The id of the user that creates the media record
     * @return Returns the media record which was created, with the ID generated for it.
     */
    public MediaRecord createMediaRecord(final List<UUID> courseIds, final CreateMediaRecordInput input,
                                         final UUID creatorId) {
        final MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

        entity.setCreatorId(creatorId);

        if (courseIds == null || courseIds.isEmpty()) {
            entity.setCourseIds(Collections.emptyList());
        } else {
            entity.setCourseIds(courseIds);
        }


        repository.save(entity);

        return removeExpiredUrlsFromMediaRecord(mapEntityToMediaRecord(entity));
    }

    /**
     * Deletes a media record matching the specified id or throws EntityNotFoundException if a record with the
     * specified id could not be found.
     *
     * @param id The id of the media record which should be deleted.
     * @return Returns the id of the record which was deleted.
     * @throws EntityNotFoundException Thrown when no record matching the passed id could be found.
     */
    @SneakyThrows
    public UUID deleteMediaRecord(final UUID id) {
        requireMediaRecordExisting(id);
        final MediaRecordEntity entity = repository.getReferenceById(id);
        final Map<String, String> minioVariables = createMinIOVariables(entity);
        final String bucketId = minioVariables.get(BUCKET_ID);
        final String filename = minioVariables.get(FILENAME);

        repository.delete(entity);

        if (doesObjectExist(filename, bucketId)) {
            minioInternalClient.removeObject(
                    RemoveObjectArgs
                            .builder()
                            .bucket(bucketId)
                            .object(filename)
                            .build());
        }

        topicPublisher.notifyMediaRecordDeleted(new MediaRecordDeletedEvent(entity.getId()));

        return id;
    }

    /**
     * Updates an existing media record matching the id passed as an attribute in the input argument.
     *
     * @param input Object containing the new attributes that should be stored for the existing media record matching
     *              the id field of the input object.
     * @return Returns the media record with its newly updated data.
     */
    public MediaRecord updateMediaRecord(final List<UUID> courseIds,
                                         final UpdateMediaRecordInput input) {
        final MediaRecordEntity oldEntity = requireMediaRecordExisting(input.getId());

        // generate new entity based on updated data
        final MediaRecordEntity newEntity = modelMapper.map(input, MediaRecordEntity.class);

        // keep creator id from old entity
        newEntity.setCreatorId(oldEntity.getCreatorId());
        // update with current courseIds
        if (courseIds == null || courseIds.isEmpty()) {
            newEntity.setCourseIds(Collections.emptyList());
        } else {
            newEntity.setCourseIds(courseIds);
        }

        // save updated entity
        final MediaRecordEntity entity = repository.save(newEntity);

        return removeExpiredUrlsFromMediaRecord(mapEntityToMediaRecord(entity));
    }

    private MediaRecord mapEntityToMediaRecord(final MediaRecordEntity entity) {
        return modelMapper.map(entity, MediaRecord.class);
    }

    /**
     * Helper method which sets the URLs of the passed media record to null if they are expired.
     * @param record The media record to remove expired URLs from.
     * @return Returns the same media record that has been passed to the method.
     */
    private MediaRecord removeExpiredUrlsFromMediaRecord(MediaRecord record) {
        if(record.getDownloadUrl() != null && isExpired(record.getDownloadUrl()))
            record.setDownloadUrl(null);

        if(record.getUploadUrl() != null && isExpired(record.getUploadUrl()))
            record.setUploadUrl(null);

        return record;
    }

    /**
     * Helper method which sets the URLs of the passed media records to null if they are expired.
     * @param records The media records to remove expired URLs from.
     * @return Returns the same list of media records that has been passed to the method.
     */
    private List<MediaRecord> removeExpiredUrlsFromMediaRecords(List<MediaRecord> records) {
        records.forEach(this::removeExpiredUrlsFromMediaRecord);
        return records;
    }

    /**
     * Checks if the download and upload url are expired and need to be recreated.
     *
     * @param url that should be checked
     * @return true if the url is expired, false otherwise
     */
    @SuppressWarnings("java:S6353") // explicit regex is more readable
    private boolean isExpired(final String url) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").withZone(ZoneOffset.UTC);
        final Pattern datePattern = Pattern.compile("X-Amz-Date=([0-9]*T[0-9]*)");
        final Matcher dateMatcher = datePattern.matcher(url);
        final Pattern expiryPattern = Pattern.compile("X-Amz-Expires=([0-9]*)");
        final Matcher expiryMatcher = expiryPattern.matcher(url);

        String dateString = "";
        while (dateMatcher.find()) {
            dateString = dateMatcher.group(1);

        }
        long expiryString = 0;
        while (expiryMatcher.find()) {
            expiryString = Long.parseLong(expiryMatcher.group(1));
        }

        final ZonedDateTime date = ZonedDateTime.parse(dateString, formatter);
        final long expiry = expiryString;

        final ZonedDateTime expiration = date.plusSeconds(expiry - 300);

        return expiration.toInstant().isBefore((Instant.now()));

    }

    /**
     * Creates a URL for uploading a file to the MinIO Server and caches it in the database entry for this media
     * record for possible future use.
     *
     * @param mediaRecord UUID of the media record to generate the upload url for.
     * @return Returns the created uploadURL.
     */
    @SneakyThrows
    public String createMediaRecordUploadUrl(final MediaRecord mediaRecord) {
        final MediaRecordEntity entity = requireMediaRecordExisting(mediaRecord.getId());
        final Map<String, String> variables = createMinIOVariables(entity);
        final String bucketId = variables.get(BUCKET_ID);
        final String filename = variables.get(FILENAME);

        final String uploadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs
                        .builder()
                        .method(Method.PUT)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
        entity.setUploadUrl(uploadUrl);
        repository.save(entity);
        return uploadUrl;
    }

    /**
     * Creates a URL for downloading a file from the MinIO Server and caches it in the database entry for this media
     * record for possible future use.
     *
     * @param mediaRecord UUID of the media record to generate the download url for.
     * @return Returns the created downloadURL.
     */
    @SneakyThrows
    public String createMediaRecordDownloadUrl(final UUID mediaRecord) {
        requireMediaRecordExisting(mediaRecord);

        final MediaRecordEntity entity = repository.getReferenceById(mediaRecord);
        final Map<String, String> variables = createMinIOVariables(entity);
        final String bucketId = variables.get(BUCKET_ID);
        final String filename = variables.get(FILENAME);

        final String downloadUrl = minioExternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
        entity.setDownloadUrl(downloadUrl);
        repository.save(entity);
        return downloadUrl;
    }

    /**
     * Generates an upload url for this media record that is only valid for internal use.
     */
    @SneakyThrows
    public String createMediaRecordInternalUploadUrl(final UUID mediaRecord) {
        requireMediaRecordExisting(mediaRecord);

        final MediaRecordEntity entity = repository.getReferenceById(mediaRecord);

        final Map<String, String> variables = createMinIOVariables(entity);
        final String bucketId = variables.get(BUCKET_ID);
        final String filename = variables.get(FILENAME);

        return minioInternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
    }

    /**
     * Generates a download url for this media record that is only valid for internal use.
     */
    @SneakyThrows
    public String createMediaRecordInternalDownloadUrl(final UUID mediaRecord) {
        requireMediaRecordExisting(mediaRecord);

        final MediaRecordEntity entity = repository.getReferenceById(mediaRecord);

        final Map<String, String> variables = createMinIOVariables(entity);
        final String bucketId = variables.get(BUCKET_ID);
        final String filename = variables.get(FILENAME);

        return minioInternalClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketId)
                        .object(filename)
                        .expiry(15, TimeUnit.MINUTES)
                        .build());
    }

    /**
     * Creates the bucketId and filename for MinIO from the media record.
     *
     * @param mediaRecord UUID of the media record
     * @return a map with the bucketID and filename which should be used by MinIO
     */
    private Map<String, String> createMinIOVariables(final MediaRecordEntity mediaRecord) {
        final Map<String, String> variables = new HashMap<>();

        final String filename = mediaRecord.getId().toString();
        variables.put(FILENAME, filename);
        final String bucketId = mediaRecord.getType().toString().toLowerCase();
        variables.put(BUCKET_ID, bucketId);

        return variables;
    }

    private boolean doesObjectExist(final String name, final String bucketName) {
        try {

            minioInternalClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
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

    /**
     * function that updates all media records that contain at least one of the received content IDs.
     * All received content Ids are removed from the media records.
     * If changes are performed to an entity, a message is published to a dapr topic.
     *
     * @param dto Event object containing a list of content IDs and a CRUD operation
     */
    public void removeContentIds(final ContentChangeEvent dto) throws IncompleteEventMessageException {

        // check if DTO is complete
        if (dto.getContentIds() == null || dto.getOperation() == null) {
            throw new IncompleteEventMessageException(IncompleteEventMessageException.ERROR_INCOMPLETE_MESSAGE);
        }

        //This method should only process Content Deletion Events
        if (!dto.getOperation().equals(CrudOperation.DELETE) || dto.getContentIds().isEmpty()) {
            return;
        }


        final List<MediaRecordEntity> entities = repository.findMediaRecordEntitiesByContentIds(dto.getContentIds());

        // apply changes to all found media records
        for (final MediaRecordEntity entity : entities) {

            //is true if changes are applied
            final boolean listChanged = entity.getContentIds().removeAll(dto.getContentIds());

            if (listChanged) {
                repository.save(entity);
            }

        }
    }

    public void publishMediaRecordFileCreatedEvent(UUID mediaRecordId) {
        topicPublisher.notifyMediaRecordFileCreated(new MediaRecordFileCreatedEvent(mediaRecordId));
    }

    /**
     * Deletes MediaRecords without a file every night at 3 am.
     */
    @Scheduled(cron = "${mediarecord.delete.cron}")
    @SneakyThrows
    private void deleteMediaRecordsWithoutAFile() {
        log.info("Running cleanup of MediaRecords");
        final List<MediaRecordEntity> records = repository.findAll();

        for (final var entity : records) {
            final Map<String, String> minioVariables = createMinIOVariables(entity);
            final String bucketId = minioVariables.get(BUCKET_ID);
            final String filename = minioVariables.get(FILENAME);

            if (!doesObjectExist(filename, bucketId)) {
                repository.delete(entity);

                topicPublisher.notifyMediaRecordDeleted(new MediaRecordDeletedEvent(entity.getId()));
            }
        }
        log.info("Cleanup completed");
    }
}
