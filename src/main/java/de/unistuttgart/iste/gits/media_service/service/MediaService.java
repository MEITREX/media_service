package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.media_service.dto.*;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.persistence.EntityNotFoundException;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class which provides the business logic of the media service.
 */
@Service
public class MediaService {

  private final MinioClient minioClient;

  /**
   * Database repository storing our media records.
   */
  private final MediaRecordRepository repository;
  /**
   * Mapper used to map media record DTOs to database Entities and vice-versa.
   */
  private final ModelMapper modelMapper;

  public MediaService(MediaRecordRepository mediaRecordRepository, ModelMapper modelMapper, MinioClient minioClient) {
    this.repository = mediaRecordRepository;
    this.modelMapper = modelMapper;
    this.minioClient = minioClient;
  }

  /**
   * @return Returns a list containing all saved media records.
   */
  public List<MediaRecordDto> getAllMediaRecords() {
    return repository.findAll().stream().map(x -> modelMapper.map(x, MediaRecordDto.class)).toList();
  }

  /**
   * When passed a list of media record ids, returns a list containing the records matching these ids, or throws
   * an EntityNotFoundException when there is no matching record for one or more of the passed ids.
   *
   * @param ids The ids to retrieve the records for.
   * @return List of the records with matching ids.
   * @throws EntityNotFoundException Thrown when one or more passed ids do not have corresponding media records in
   *                                 the database.
   */
  public List<MediaRecordDto> getMediaRecordsById(List<UUID> ids) {
    List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

    // if there are fewer returned records than passed ids, that means that some ids could not be found in the
    // db. In that case, calculate the difference of the two lists and throw an exception listing which ids
    // could not be found
    if (records.size() != ids.size()) {
      List<UUID> missingIds = new ArrayList<>(ids);
      missingIds.removeAll(records.stream().map(MediaRecordEntity::getId).toList());

      throw new EntityNotFoundException("Media record(s) with id(s) "
          + missingIds.stream().map(UUID::toString).collect(Collectors.joining(", "))
          + " not found.");
    }

    return records.stream().map(x -> modelMapper.map(x, MediaRecordDto.class)).toList();
  }

  /**
   * Creates a new media record with the attributes specified in the input argument.
   *
   * @param input Object storing the attributes the newly created media record should have.
   * @return Returns the media record which was created, with the ID generated for it.
   */
  public MediaRecordDto createMediaRecord(CreateMediaRecordInputDto input) {
    MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

    repository.save(entity);

    return modelMapper.map(entity, MediaRecordDto.class);
  }

  /**
   * Deletes a media record matching the specified id or throws EntityNotFoundException if a record with the
   * specified id could not be found.
   *
   * @param id The id of the media record which should be deleted.
   * @return Returns the id of the record which was deleted.
   * @throws EntityNotFoundException Thrown when no record matching the passed id could be found.
   */
  public UUID deleteMediaRecord(UUID id) {
    Optional<MediaRecordEntity> entity = repository.findById(id);

    repository.delete(entity.orElseThrow(() -> new EntityNotFoundException("Media record with id "
        + id + " not found.")));

    return id;
  }

  /**
   * Updates an existing media record matching the id passed as an attribute in the input argument.
   *
   * @param input Object containing the new attributes that should be stored for the existing media record matching
   *              the id field of the input object.
   * @return Returns the media record with its newly updated data.
   */
  public MediaRecordDto updateMediaRecord(UpdateMediaRecordInputDto input) {
    if (!repository.existsById(input.getId())) {
      throw new EntityNotFoundException("Media record with id " + input.getId() + " not found.");
    }

    MediaRecordEntity entity = repository.save(modelMapper.map(input, MediaRecordEntity.class));

    MediaRecordEntity updatedRecord = repository.save(entity);

    return modelMapper.map(updatedRecord, MediaRecordDto.class);
  }

  /**
   *  Creates an URL for uploading a file to the minIO Server.
   * @param input DTO which contains the bucket id to which to upload as well as the name of the file which should be uploaded.
   * @return Returns the created uploadURL.
   */
  @SneakyThrows
  public Storage_UploadUrlDto createUploadUrl(InputStorage_CreateUrlDto input) {
    String url = minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket(input.getBucketId())
            .object(input.getFileName())
            .build()
    );
    Storage_UploadUrlDto uploadUrlDto = new Storage_UploadUrlDto();
    uploadUrlDto.setUrl(url);
    return uploadUrlDto;
  }
  /**
   * Creates an URL for downloading a file from the MinIO Server.
   * @param input DTO which contains the bucket id from which to download as well as the name of the file which should be downloaded.
   * @return Returns the created downloadURL..
   */
  @SneakyThrows
  public Storage_DownloadUrlDto creatDownloadUrl(InputStorage_CreateUrlDto input) {
    String url = minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(input.getBucketId())
            .object(input.getFileName())
            .build()
    );
    Storage_DownloadUrlDto downloadUrlDto = new Storage_DownloadUrlDto();
    downloadUrlDto.setUrl(url);
    return downloadUrlDto;
  }

  /**
   * Creates a new bucket on the MinIO Server.
   * @param input DTO which contains the bucketID (name of the bucket) which should be created
   * @return true, if the creation was successful, false otherwise
   */
  @SneakyThrows
  public boolean createBucket(InputStorage_CreateBucketDto input)  {
    boolean found = minioClient.bucketExists(
        BucketExistsArgs.builder()
            .bucket(input.getBucketId())
            .build());
    if (!found) {
      minioClient.makeBucket(
          MakeBucketArgs.builder()
              .bucket(input.getBucketId())
              .build());
      return true;
    } else {
      System.out.println("Bucket " + input.getBucketId() + " already exists!");
      return false;
    }

  }
}
