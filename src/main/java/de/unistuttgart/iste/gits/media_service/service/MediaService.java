package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDTO;
import de.unistuttgart.iste.gits.media_service.dto.MediaTypeDTO;
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

    public List<MediaRecordDTO> getAllMediaRecords() {
        return mediaRecordRepository.findAll().stream().map(this::convertEntityToDTO).toList();
    }

    public MediaRecordDTO convertEntityToDTO(MediaRecordEntity entity) {
        return modelMapper.map(entity, MediaRecordDTO.class);
    }
}
