package de.unistuttgart.iste.gits.media_service.controller;

import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDTO;
import de.unistuttgart.iste.gits.media_service.service.MediaService;
import lombok.extern.slf4j.Slf4j;
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
    public List<MediaRecordDTO> media() {
        log.debug("media() requested.");

        return mediaService.getAllMediaRecords();
    }
}
