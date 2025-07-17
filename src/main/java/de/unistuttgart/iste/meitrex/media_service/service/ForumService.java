package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ForumActivity;
import de.unistuttgart.iste.meitrex.common.event.ForumActivityEvent;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ModelMapper modelMapper;

    private final ForumRepository forumRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final ThreadContentReferenceRepository threadContentReferenceRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final TopicPublisher topicPublisher;

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

    public List<Thread> getThreadsByThreadContentReferences(List<ThreadContentReferenceEntity> threadContentReferenceEntities) {
        return threadContentReferenceEntities.stream().map(ThreadContentReferenceEntity::getThread)
                .map(threadMapper::mapThread).toList();
    }

    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        PostEntity postEntity = new PostEntity(post.getContent(), userId ,thread);
        postEntity = postRepository.save(postEntity);
        thread.getPosts().add(postEntity);
        thread.setNumberOfPosts(thread.getNumberOfPosts() + 1);
        threadRepository.save(thread);
        ForumActivityEvent event = ForumActivityEvent.builder()
                .forumId(thread.getForum().getId())
                .courseId(thread.getForum().getCourseId())
                .userId(userId)
                .build();
        if (thread instanceof QuestionThreadEntity) {
            event.setActivity(ForumActivity.ANSWER);
        } else if (thread instanceof InfoThreadEntity){
            event.setActivity(ForumActivity.INFO);
        }

        topicPublisher.notifyForumActivity(event);
        return modelMapper.map(postEntity, Post.class);
    }

    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getUpvotedByUsers().contains(userId)) {
            postEntity.getUpvotedByUsers().add(userId);
        } else {
            postEntity.getUpvotedByUsers().remove(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getDownvotedByUsers().contains(userId)) {
            postEntity.getDownvotedByUsers().add(userId);
        } else {
            postEntity.getDownvotedByUsers().remove(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public QuestionThread createQuestionThread(InputQuestionThread thread, ForumEntity forum ,UUID userId) {
        PostEntity questionEntity = new PostEntity(thread.getQuestion().getContent(), userId);
        QuestionThreadEntity threadEntity = new QuestionThreadEntity(forum, userId, thread.getTitle(), questionEntity);
        questionEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }
        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        topicPublisher.notifyForumActivity(ForumActivityEvent.builder()
                        .userId(userId)
                        .forumId(forum.getId())
                        .courseId(forum.getCourseId())
                        .activity(ForumActivity.QUESTION)
                .build());
        return modelMapper.map(threadEntity, QuestionThread.class);
    }

    public InfoThread createInfoThread(InputInfoThread thread, ForumEntity forum ,UUID userId) {
        PostEntity infoEntity = new PostEntity(thread.getInfo().getContent(), userId);
        InfoThreadEntity threadEntity = new InfoThreadEntity(forum, userId, thread.getTitle(), infoEntity);
        infoEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }

        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        topicPublisher.notifyForumActivity(ForumActivityEvent.builder()
                .userId(userId)
                .forumId(forum.getId())
                .courseId(forum.getCourseId())
                .activity(ForumActivity.THREAD)
                .build());

        return modelMapper.map(threadEntity, InfoThread.class);
    }

    private void addThreatToContentOnThreadCreation(ThreadEntity thread, UUID contentId, Integer timeStampSeconds, Integer pageNumber) {
        List<MediaRecordEntity> mediaRecordEntities = mediaRecordRepository
                .findMediaRecordEntitiesByContentIds(List.of(contentId));
        mediaRecordEntities.stream().findAny().orElseThrow(()-> new EntityNotFoundException("MediaRecord that includes content with the id "
                + contentId + " not found"));
        mediaRecordEntities.stream().map(MediaRecordEntity::getCourseIds).flatMap(List::stream)
                .filter(courseId -> courseId.equals(thread.getForum().getCourseId())).findAny().orElseThrow(() ->
                        new EntityNotFoundException("Content with the id " + contentId
                                + " not in course " + thread.getForum().getCourseId()));
        thread.setThreadContentReference(new ThreadContentReferenceEntity(thread, contentId, timeStampSeconds, pageNumber));
    }

    public Post updatePost(InputPost post, PostEntity postEntity, UUID userId) throws AuthenticationException {
        if (!postEntity.getAuthorId().equals(userId)) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        postEntity.setContent(post.getContent());
        postEntity.setEdited(true);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post deletePost(PostEntity post, LoggedInUser user) throws AuthenticationException {
        if (!post.getAuthorId().equals(user.getId())
                && !(user.getRealmRoles().contains(LoggedInUser.RealmRole.COURSE_CREATOR)
                || user.getRealmRoles().contains(LoggedInUser.RealmRole.SUPER_USER))) {
            throw new AuthenticationException("User is not authorized to delete this post");
        }
        Post realPost = modelMapper.map(post, Post.class);
        ThreadEntity thread = post.getThread();
        thread.getPosts().remove(post);
        thread.setNumberOfPosts(thread.getNumberOfPosts() - 1);
        threadRepository.save(thread);
        post.setThread(null);
        postRepository.delete(post);
        return realPost;
    }

    public Thread deleteThread(ThreadEntity thread, LoggedInUser user) throws AuthenticationException {
        if (!thread.getCreatorId().equals(user.getId())
                && !(user.getRealmRoles().contains(LoggedInUser.RealmRole.COURSE_CREATOR)
                || user.getRealmRoles().contains(LoggedInUser.RealmRole.SUPER_USER))) {
            throw new AuthenticationException("User is not authorized to delete this thread");
        }
        Thread realThread = threadMapper.mapThread(thread);
        ForumEntity forum = thread.getForum();
        forum.getThreads().remove(thread);
        threadRepository.delete(thread);
        forumRepository.save(forum);
        return realThread;
    }

    public ThreadContentReference addThreadToContent(ThreadEntity thread, UUID contentId, Integer timeStamp, Integer pageNumber) {
        ThreadContentReferenceEntity threadContentReferenceEntity = new ThreadContentReferenceEntity(thread, contentId, timeStamp, pageNumber);
        threadContentReferenceRepository.save(threadContentReferenceEntity);
        thread.setThreadContentReference(threadContentReferenceEntity);
        threadRepository.save(thread);
        return modelMapper.map(threadContentReferenceEntity, ThreadContentReference.class);
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
