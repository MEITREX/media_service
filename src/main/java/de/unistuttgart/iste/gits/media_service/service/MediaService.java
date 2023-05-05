package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaTypeDto;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MediaService {

    private final MediaRecordRepository mediaRecordRepository;
    private final ModelMapper modelMapper;

    public MediaService(MediaRecordRepository mediaRecordRepository, ModelMapper modelMapper) {
        this.mediaRecordRepository = mediaRecordRepository;
        this.modelMapper = modelMapper;
    }

    public List<MediaRecordDto> getAllMediaRecords() {
        return mediaRecordRepository.findAll().stream().map(this::convertEntityToDto).toList();
    }

    public MediaRecordDto createMediaRecord(String mediaName, MediaTypeDto mediaType) {
        MediaRecordEntity entity = new MediaRecordEntity(
                UUID.randomUUID(),
                mediaName,
                modelMapper.map(mediaType, MediaRecordEntity.MediaType.class)
        );

        mediaRecordRepository.save(entity);

        return convertEntityToDto(entity);
    }

    public MediaRecordDto convertEntityToDto(MediaRecordEntity entity) {
        return modelMapper.map(entity, MediaRecordDto.class);
    }
}
