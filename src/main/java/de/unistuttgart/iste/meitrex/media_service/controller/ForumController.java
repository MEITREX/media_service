package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.media_service.service.ForumService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import javax.naming.AuthenticationException;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourses;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ForumController {
    private final ForumService forumService;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final ModelMapper modelMapper;
    private final ForumRepository forumRepository;
    private static final String NOT_FOUND = " not found";
    private static final String THREAD = "Thread with id ";
    private final ThreadMapper threadMapper;
    private final QuestionThreadRepository questionThreadRepository;


    @QueryMapping
    public Forum forum(@Argument final UUID id,
                              @ContextValue final LoggedInUser currentUser) {
        Forum forum = forumService.getForumById(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,forum.getCourseId());
        return forum;
    }

    @QueryMapping
    public Thread thread(@Argument final UUID id,
                            @ContextValue final LoggedInUser currentUser) {
        ThreadEntity thread = forumService.getThreadById(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,thread.getForum().getCourseId());
        return threadMapper.mapThread(thread);
    }

    @QueryMapping
    public List<Thread> threadsByMediaRecord(@Argument UUID id,
                                             @ContextValue final LoggedInUser currentUser) {
        MediaRecordEntity mediaRecord = mediaRecordRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("MediaRecord with id " + id + NOT_FOUND));
        if (mediaRecord.getCourseIds() != null && !mediaRecord.getCourseIds().isEmpty()) {
            validateUserHasAccessToCourses(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, mediaRecord.getCourseIds());
        }
        return forumService.getThreadsByMediaRecord(mediaRecord);
    }

    @QueryMapping
    public Forum forumByCourseId(@Argument final UUID id,
                          @ContextValue final LoggedInUser currentUser) {
        Forum forum = forumService.getForumByCourseId(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,forum.getCourseId());
        return forum;
    }

    @MutationMapping
    public QuestionThread createQuestionThread(@Argument final InputQuestionThread thread,
                               @ContextValue final LoggedInUser currentUser) {
        ForumEntity forum = forumRepository.findById(thread.getForumId()).orElseThrow(() ->
                new EntityNotFoundException("Forum with the id " + thread.getForumId() + NOT_FOUND));
        if (forum.getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, forum.getCourseId());
        }
        return forumService.createQuestionThread(thread, forum, currentUser.getId());
    }

    @MutationMapping
    public InfoThread createInfoThread(@Argument final InputInfoThread thread,
                                       @ContextValue final LoggedInUser currentUser) {
        ForumEntity forum = forumRepository.findById(thread.getForumId()).orElseThrow(() ->
                new EntityNotFoundException("Forum with the id " + thread.getForumId() + NOT_FOUND));
        if (forum.getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, forum.getCourseId());
        }
        return forumService.createInfoThread(thread, forum, currentUser.getId());
    }

    @MutationMapping
    public Post addPost(@Argument final InputPost post,
                        @ContextValue final LoggedInUser currentUser) {
        ThreadEntity thread = threadRepository.findById(post.getThreadId()).orElseThrow(()->
                new EntityNotFoundException(THREAD + post.getThreadId() + NOT_FOUND));

        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, thread.getForum().getCourseId());

        return forumService.addPostToThread(post, thread , currentUser.getId());
    }

    @MutationMapping
    public Post upvotePost(@Argument final UUID postId,
                        @ContextValue final LoggedInUser currentUser) {
        PostEntity post = checkUserInCourse(postId, currentUser);

        return forumService.upvotePost(post, currentUser.getId());
    }

    @MutationMapping
    public Post downvotePost(@Argument final UUID postId,
                           @ContextValue final LoggedInUser currentUser) {
        PostEntity post = checkUserInCourse(postId, currentUser);

        return forumService.downvotePost(post, currentUser.getId());
    }

    @MutationMapping
    public Post updatePost(@Argument final InputPost post,
                           @ContextValue final LoggedInUser currentUser) throws AuthenticationException {
        PostEntity postEntity = checkUserInCourse(post.getId(), currentUser);
        return forumService.updatePost(post, postEntity, currentUser.getId());
    }

    @MutationMapping
    public Post deletePost(@Argument final UUID postId,
                           @ContextValue final LoggedInUser currentUser) throws AuthenticationException {
        PostEntity postEntity = checkUserInCourse(postId, currentUser);
        return forumService.deletePost(postEntity, currentUser);
    }

    @MutationMapping
    public ThreadMediaRecordReference addThreadToMediaRecord(@Argument final InputThreadMediaRecordReference threadMediaRecordReference,
                                                             @ContextValue final LoggedInUser currentUser) {
        ThreadEntity thread = threadRepository.findById(threadMediaRecordReference.getThreadId()).orElseThrow(()->
                new EntityNotFoundException(THREAD + threadMediaRecordReference.getThreadId() + NOT_FOUND));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, thread.getForum().getCourseId());
        MediaRecordEntity mediaRecord = mediaRecordRepository.findById(threadMediaRecordReference.getMediaRecordId()).orElseThrow(()->
                new EntityNotFoundException("MediaRecord with the id "  + threadMediaRecordReference.getMediaRecordId() + NOT_FOUND));
        return forumService.addThreadToMediaRecord(thread, mediaRecord, threadMediaRecordReference.getTimeStampSeconds(), threadMediaRecordReference.getPageNumber());
    }

    @MutationMapping
    public QuestionThread selectAnswer(@Argument final UUID postId){
        PostEntity post = postRepository.findById(postId).orElseThrow(
                () -> new EntityNotFoundException("Post with the id " + postId + NOT_FOUND)
        );
        log.info(post.getThread().toString());
        QuestionThreadEntity questionThread = questionThreadRepository.findById(post.getThread().getId()).orElseThrow(
                () -> new EntityNotFoundException("QuestionThread with the id " + post.getThread().getId() + NOT_FOUND));
        return forumService.addAnserToQuestionThread(questionThread, post);
    }

    private PostEntity checkUserInCourse(@Argument UUID postId, @ContextValue LoggedInUser currentUser) {
        PostEntity post = postRepository.findById(postId).orElseThrow(() ->
                new EntityNotFoundException("Post with the id"  + postId + NOT_FOUND));
        ThreadEntity thread = threadRepository.findById(post.getThread().getId()).orElseThrow(()->
                new EntityNotFoundException(THREAD + post.getThread().getId() + NOT_FOUND));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT, thread.getForum().getCourseId());
        return post;
    }
}
