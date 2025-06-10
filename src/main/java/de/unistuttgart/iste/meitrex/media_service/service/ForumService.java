package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    private final ModelMapper modelMapper;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final ThreadMediaRecordReferenceRepository threadMediaRecordReferenceRepository;

    public Forum getForumById(UUID id) {
        return modelMapper.map(forumRepository.findById(id).orElse(null), Forum.class);
    }

    public Forum getForumByCourseId(UUID id) {
        ForumEntity forum = forumRepository.findByCourseId(id).orElseGet(() -> createForum(id));
        return modelMapper.map(forum, Forum.class);
    }

    public ThreadEntity getThreadById(UUID id) {
        ThreadEntity thread = threadRepository.findById(id).orElseThrow(()->
                new EntityNotFoundException("Thread with the id" + id + "not found"));
        if (thread instanceof QuestionThreadEntity questionThread) {
            log.info(questionThread.getQuestion().getId() + "");
        }
        return thread;
    }

    public List<Thread> getThreadsByMediaRecord(MediaRecordEntity mediaRecord) {
        return mediaRecord.getThreadMediaRecordReference().stream()
                .map((record) -> modelMapper.map(record.getThread(), Thread.class)).toList();
    }

    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        PostEntity postEntity = new PostEntity(post.getContent(), userId ,thread);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        postEntity.getUpvotedByUsers().add(userId);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        postEntity.getDownvotedByUsers().add(userId);
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
        postRepository.delete(post);
        return modelMapper.map(post, Post.class);
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
