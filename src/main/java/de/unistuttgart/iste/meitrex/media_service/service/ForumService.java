package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ForumRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.PostRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ThreadRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    private final ModelMapper modelMapper;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;

    public Forum getForumById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(createForum(id)), Forum.class);
    }

    public Forum getForumByCourseId(UUID id) {
        return modelMapper.map(forumRepository.findByCourseId(id).orElse(createForum(id)), Forum.class);
    }

    public Thread getThreadById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Thread.class);
    }

    public Post addPostToThread(Post post) {
        threadRepository.findById(post.getThread().getId()).orElseThrow(()->
                new EntityNotFoundException("Thread with the id"  + post.getThread().getId() + "not found"));
        PostEntity postEntity = modelMapper.map(post, PostEntity.class);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post upvotePost(Post post, UUID userId) {
        PostEntity postEntity = postRepository.findById(post.getId()).orElseThrow(() ->
                new EntityNotFoundException("Post with the id" + post.getId() + "not found"));
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        postEntity.getUpvotedByUsers().add(userId);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(Post post, UUID userId) {
        PostEntity postEntity = postRepository.findById(post.getId()).orElseThrow(() ->
                new EntityNotFoundException("Post with the id" + post.getId() + "not found"));
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        postEntity.getDownvotedByUsers().add(userId);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public QuestionThread createQuestionThread(QuestionThread questionThread) {
        ForumEntity forumEntity = forumRepository.findById(questionThread.getForum().getId()).orElseThrow(()->
                new EntityNotFoundException("Forum with the id" + questionThread.getForum().getId() + "not found"));
        QuestionThreadEntity questionThreadEntity = modelMapper.map(questionThread, QuestionThreadEntity.class);
        forumEntity.getThreads().add(questionThreadEntity);
        forumRepository.save(forumEntity);
        return modelMapper.map(threadRepository.save(questionThreadEntity), QuestionThread.class);
    }

    public InfoThread createInfoThread(InfoThread infoThread) {
        ForumEntity forumEntity = forumRepository.findById(infoThread.getForum().getId()).orElseThrow(()->
                new EntityNotFoundException("Forum with the id" + infoThread.getForum().getId() + "not found"));
        InfoThreadEntity infoThreadEntity = modelMapper.map(infoThread, InfoThreadEntity.class);
        forumEntity.getThreads().add(infoThreadEntity);
        forumRepository.save(forumEntity);
        return modelMapper.map(threadRepository.save(infoThreadEntity), InfoThread.class);
    }

    private ForumEntity createForum(UUID courseId) {
        return new ForumEntity(courseId);
    }
}
