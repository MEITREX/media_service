package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.media_service.dto.CreateMediaRecordInputDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaTypeDto;
import de.unistuttgart.iste.gits.media_service.dto.UpdateMediaRecordInputDto;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

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
}
