package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.generated.dto.ThreadContentReference;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.QuestionThreadEntity;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionThreadMapper {
    private final PostMapper postMapper;
    private final ModelMapper modelMapper;

    public QuestionThread mapQuestionThread(QuestionThreadEntity questionThreadEntity) {
        QuestionThread questionThread = QuestionThread.builder()
                .setId(questionThreadEntity.getId())
                .setCreatorId(questionThreadEntity.getCreatorId())
                .setTitle(questionThreadEntity.getTitle())
                .setCreationTime(questionThreadEntity.getCreationTime())
                .setPosts(postMapper.mapToPosts(questionThreadEntity.getPosts()))
                .setNumberOfPosts(questionThreadEntity.getNumberOfPosts())
                .setThreadContentReference(null)
                .setQuestion(null)
                .build();
        if (questionThreadEntity.getQuestion() != null) {
            questionThread.setQuestion(postMapper.mapToPost(questionThreadEntity.getQuestion()));
        }
        if (questionThreadEntity.getSelectedAnswer() != null) {
            questionThread.setSelectedAnswer(postMapper.mapToPostWithThread(questionThreadEntity.getSelectedAnswer(), questionThreadEntity));
        }
        if (questionThreadEntity.getThreadContentReference() != null) {
            questionThread.setThreadContentReference(modelMapper.map(questionThreadEntity.getThreadContentReference(), ThreadContentReference.class));
        }
        return questionThread;
    }
}
