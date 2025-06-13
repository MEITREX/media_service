package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ModelMapper modelMapper;

    private final ForumRepository forumRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final ThreadMediaRecordReferenceRepository threadMediaRecordReferenceRepository;

    private final ForumMapper forumMapper;
    private final ThreadMapper threadMapper;

    public Forum getForumById(UUID id) {
        return forumMapper.forumEntityToForum(forumRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Forum with the id " + id + " not found")));
    }

    public Forum getForumByCourseId(UUID id) {
        ForumEntity forum = forumRepository.findByCourseId(id).orElseGet(() -> createForum(id));
        return forumMapper.forumEntityToForum(forum);
    }

    public ThreadEntity getThreadById(UUID id) {
        return threadRepository.findById(id).orElseThrow(()->
                new EntityNotFoundException("Thread with the id " + id + " not found"));
    }

    public List<Thread> getThreadsByMediaRecord(MediaRecordEntity mediaRecord) {
        return threadMediaRecordReferenceRepository.findAllByMediaRecord(mediaRecord).parallelStream()
                .map(pRecordEntity -> threadMapper.mapThread(pRecordEntity.getThread())).toList();
    }

    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        PostEntity postEntity = new PostEntity(post.getContent(), userId ,thread);
        postEntity = postRepository.save(postEntity);
        thread.getPosts().add(postEntity);
        thread.setNumberOfPosts(thread.getNumberOfPosts() + 1);
        threadRepository.save(thread);
        return modelMapper.map(postEntity, Post.class);
    }

    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getUpvotedByUsers().contains(userId)) {
            postEntity.getUpvotedByUsers().add(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getDownvotedByUsers().contains(userId)) {
            postEntity.getDownvotedByUsers().add(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public QuestionThread createQuestionThread(InputQuestionThread thread, ForumEntity forum ,UUID userId) {
        PostEntity questionEntity = new PostEntity(thread.getQuestion().getContent(), userId);
        QuestionThreadEntity threadEntity = new QuestionThreadEntity(forum, userId, thread.getTitle(), questionEntity);
        questionEntity.setThread(threadEntity);
        postRepository.save(questionEntity);

        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        return modelMapper.map(threadEntity, QuestionThread.class);
    }

    public InfoThread createInfoThread(InputInfoThread thread, ForumEntity forum ,UUID userId) {
        PostEntity infoEntity = new PostEntity(thread.getInfo().getContent(), userId);
        InfoThreadEntity threadEntity = new InfoThreadEntity(forum, userId, thread.getTitle(), infoEntity);
        infoEntity.setThread(threadEntity);
        postRepository.save(infoEntity);

        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        return modelMapper.map(threadEntity, InfoThread.class);
    }

    public Post updatePost(InputPost post, PostEntity postEntity, UUID userId) throws AuthenticationException {
        if (!postEntity.getAuthorId().equals(userId)) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        postEntity.setContent(post.getContent());
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post deletePost(PostEntity post, LoggedInUser user) throws AuthenticationException {
        if (!post.getAuthorId().equals(user.getId())) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        ThreadEntity thread = post.getThread();
        thread.getPosts().remove(post);
        thread.setNumberOfPosts(thread.getNumberOfPosts() - 1);
        threadRepository.save(thread);
        postRepository.delete(post);
        return modelMapper.map(post, Post.class);
    }

    public ThreadMediaRecordReference addThreadToMediaRecord(ThreadEntity thread, MediaRecordEntity mediaRecord, Integer timeStamp, Integer pageNumber) {
        ThreadMediaRecordReferenceEntity threadMediaRecordReferenceEntity = new ThreadMediaRecordReferenceEntity(thread, mediaRecord, timeStamp, pageNumber);
        threadMediaRecordReferenceRepository.save(threadMediaRecordReferenceEntity);
        thread.setThreadMediaRecordReference(threadMediaRecordReferenceEntity);
        threadRepository.save(thread);
        return modelMapper.map(threadMediaRecordReferenceEntity, ThreadMediaRecordReference.class);
    }

    public QuestionThread addAnserToQuestionThread(QuestionThreadEntity questionThread, PostEntity answer) {
        questionThread.setSelectedAnswer(answer);
        questionThread = threadRepository.save(questionThread);
        return modelMapper.map(questionThread, QuestionThread.class);
    }

    private ForumEntity createForum(UUID courseId) {
        ForumEntity forum = new ForumEntity(courseId);
        forum = forumRepository.save(forum);
        return forum;
    }
}
