package de.unistuttgart.iste.gits.media_service.service;

import de.unistuttgart.iste.gits.media_service.dto.CreateMediaRecordInputDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaRecordDto;
import de.unistuttgart.iste.gits.media_service.dto.MediaTypeDto;
import de.unistuttgart.iste.gits.media_service.dto.UpdateMediaRecordInputDto;
import de.unistuttgart.iste.gits.media_service.persistence.dao.MediaRecordEntity;
import de.unistuttgart.iste.gits.media_service.persistence.repository.MediaRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaRecordRepository repository;
    private final ModelMapper modelMapper;

    public MediaService(MediaRecordRepository mediaRecordRepository, ModelMapper modelMapper) {
        this.repository = mediaRecordRepository;
        this.modelMapper = modelMapper;
    }

    public List<MediaRecordDto> getAllMediaRecords() {
        return repository.findAll().stream().map(x -> modelMapper.map(x, MediaRecordDto.class)).toList();
    }

    public List<MediaRecordDto> getMediaRecordsById(List<UUID> ids) {
        List<MediaRecordEntity> records = repository.findAllById(ids).stream().toList();

        if(records.size() != ids.size()) {
            List<UUID> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(records.stream().map(MediaRecordEntity::getId).toList());

            throw new EntityNotFoundException("Media record(s) with id(s) "
                    + missingIds.stream().map(UUID::toString).collect(Collectors.joining(", "))
                    + " not found.");
        }

        return records.stream().map(x -> modelMapper.map(x, MediaRecordDto.class)).toList();
    }

    public MediaRecordDto createMediaRecord(CreateMediaRecordInputDto input) {
        MediaRecordEntity entity = modelMapper.map(input, MediaRecordEntity.class);

        repository.save(entity);

        return modelMapper.map(entity, MediaRecordDto.class);
    }

    public UUID deleteMediaRecord(UUID id) {
        Optional<MediaRecordEntity> entity = repository.findById(id);

        repository.delete(entity.orElseThrow(() -> new EntityNotFoundException("Media record with id "
                + id + " not found.")));

        return id;
    }

    public MediaRecordDto updateMediaRecord(UpdateMediaRecordInputDto input) {
        if(!repository.existsById(input.getId())) {
            throw new EntityNotFoundException("Media record with id " + input.getId() + " not found.");
        }

        MediaRecordEntity entity = repository.save(modelMapper.map(input, MediaRecordEntity.class));

        MediaRecordEntity updatedRecord = repository.save(entity);

        return modelMapper.map(updatedRecord, MediaRecordDto.class);
    }
}
