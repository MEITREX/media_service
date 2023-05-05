package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaTypeDto;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @QueryMapping
    public List<MediaRecordDto> media() {
        log.debug("media() requested.");

        return mediaService.getAllMediaRecords();
    }

    @MutationMapping
    public MediaRecordDto createMediaRecord(@Argument String mediaName, @Argument MediaTypeDto mediaType) {
        return mediaService.createMediaRecord(mediaName, mediaType);
    }
}
