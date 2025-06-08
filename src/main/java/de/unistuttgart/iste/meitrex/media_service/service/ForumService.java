package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.Forum;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    private final ModelMapper modelMapper;

    public Forum getForumById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Forum.class);
    }


    public Thread getThreadById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Thread.class);
    }
}
