package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.media_service.dto.*;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

/**
 * Implements the graphql endpoints of the service.
 */
@Slf4j
@Controller
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @QueryMapping
    public List<MediaRecordDto> mediaRecords() {
        return mediaService.getAllMediaRecords();
    }

    @QueryMapping
    public List<MediaRecordDto> mediaRecordsById(@Argument List<UUID> ids) {
        return mediaService.getMediaRecordsById(ids);
    }

    @MutationMapping
    public MediaRecordDto createMediaRecord(@Argument CreateMediaRecordInputDto input) {
        return mediaService.createMediaRecord(input);
    }

    @MutationMapping
    public UUID deleteMediaRecord(@Argument UUID id) {
        return mediaService.deleteMediaRecord(id);
    }

    @MutationMapping
    public MediaRecordDto updateMediaRecord(@Argument UpdateMediaRecordInputDto input) {
        return mediaService.updateMediaRecord(input);
    }

    @MutationMapping
    public UploadUrlDto createStorageUploadUrl(@Argument CreateUrlInputDto input) {
        return mediaService.createUploadUrl(input);
    }
    @MutationMapping
    public DownloadUrlDto createStorageDownloadUrl(@Argument CreateUrlInputDto input) {
        return mediaService.createDownloadUrl(input);
    }
    @MutationMapping
    public boolean createStorageBucket(@Argument CreateBucketInputDto input) {
        return mediaService.createBucket(input);
    }
}
