package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    private final ModelMapper modelMapper;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final QuestionThreadRepository questionThreadRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final ThreadMediaRecordReferenceRepository threadMediaRecordReferenceRepository;

    public Forum getForumById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Forum.class);
    }

    public Forum getForumByCourseId(UUID id) {
        ForumEntity forum = forumRepository.findByCourseId(id).orElseGet(() -> createForum(id));
        return modelMapper.map(forum, Forum.class);
    }

    public Thread getThreadById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Thread.class);
    }

    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        QuestionThreadEntity question = null;
        QuestionThreadEntity selectedAnswer = null;
        if (post.getQuestion() != null) {
            question = questionThreadRepository.findById(post.getQuestion()).orElse(null);
        }
        if (post.getSelectedAnswer() != null) {
            selectedAnswer = questionThreadRepository.findById(post.getSelectedAnswer()).orElse(null);
        }
        PostEntity postEntity = new PostEntity(post.getTitle(), post.getContent(), userId ,thread, question, selectedAnswer);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        postEntity.getUpvotedByUsers().add(userId);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        log.info(postEntity.getDownvotedByUsers().size() + "");
        postEntity.getDownvotedByUsers().add(userId);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public QuestionThread createQuestionThread(InputQuestionThread questionThread, UUID userId) {
        ForumEntity forumEntity = forumRepository.findById(questionThread.getForum().getId()).orElseThrow(()->
                new EntityNotFoundException("Forum with the id" + questionThread.getForum().getId() + "not found"));
        QuestionThreadEntity questionThreadEntity = new QuestionThreadEntity(forumEntity, userId, questionThread.getTitle());
        questionThreadEntity = threadRepository.save(questionThreadEntity);
        forumEntity.getThreads().add(questionThreadEntity);
        forumRepository.save(forumEntity);
        return modelMapper.map(questionThreadEntity, QuestionThread.class);
    }

    public InfoThread createInfoThread(InputInfoThread infoThread, UUID userId) {
        ForumEntity forumEntity = forumRepository.findById(infoThread.getForum().getId()).orElseThrow(()->
                new EntityNotFoundException("Forum with the id" + infoThread.getForum().getId() + "not found"));
        InfoThreadEntity infoThreadEntity = new InfoThreadEntity(forumEntity, userId, infoThread.getTitle());
        infoThreadEntity = threadRepository.save(infoThreadEntity);
        forumEntity.getThreads().add(infoThreadEntity);
        forumRepository.save(forumEntity);
        return modelMapper.map(infoThreadEntity, InfoThread.class);
    }

    public ThreadMediaRecordReference addThreadToMediaRecord(ThreadEntity thread, MediaRecordEntity mediaRecord, int timeStamp, int pageNumber) {
        ThreadMediaRecordReferenceEntity threadMediaRecordReferenceEntity = new ThreadMediaRecordReferenceEntity(thread, mediaRecord, timeStamp, pageNumber);
        mediaRecord.getThreadMediaRecordReference().add(threadMediaRecordReferenceEntity);
        thread.setThreadMediaRecordReference(threadMediaRecordReferenceEntity);
        mediaRecordRepository.save(mediaRecord);
        threadRepository.save(thread);
        threadMediaRecordReferenceRepository.save(threadMediaRecordReferenceEntity);
        return modelMapper.map(threadMediaRecordReferenceEntity, ThreadMediaRecordReference.class);
    }

    private ForumEntity createForum(UUID courseId) {
        ForumEntity forum = new ForumEntity(courseId);
        forum = forumRepository.save(forum);
        return forum;
    }
}
