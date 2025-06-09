package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.PostRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ThreadRepository;
import de.unistuttgart.iste.meitrex.media_service.service.ForumService;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.GlobalPermissionAccessValidator.validateUserHasGlobalPermission;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourses;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ForumController {
    private final ForumService forumService;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final MediaController mediaController;
    private final MediaRecordRepository mediaRecordRepository;

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
        Thread thread = forumService.getThreadById(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,thread.getForum().getCourseId());
        return thread;
    }

    @QueryMapping
    public Forum forumByCourseId(@Argument final UUID id,
                          @ContextValue final LoggedInUser currentUser) {
        Forum forum = forumService.getForumByCourseId(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,forum.getCourseId());
        return forum;
    }

    @MutationMapping
    public QuestionThread createQuestionThread(@Argument final InputQuestionThread questionThread,
                               @ContextValue final LoggedInUser currentUser) {
        if (questionThread.getForum().getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, questionThread.getForum().getCourseId());
        }
        return forumService.createQuestionThread(questionThread, currentUser.getId());
    }

    @MutationMapping
    public InfoThread createInfoThread(@Argument final InputInfoThread infoThread,
                                           @ContextValue final LoggedInUser currentUser) {
        if (infoThread.getForum().getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, infoThread.getForum().getCourseId());
        }
        return forumService.createInfoThread(infoThread, currentUser.getId());
    }

    @MutationMapping
    public Post addPost(@Argument final InputPost post,
                        @ContextValue final LoggedInUser currentUser) {
        ThreadEntity thread = threadRepository.findById(post.getThreadId()).orElseThrow(()->
                new EntityNotFoundException("Thread with the id"  + post.getThreadId() + "not found"));

        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, thread.getForum().getCourseId());

        return forumService.addPostToThread(post, thread , currentUser.getId());
    }

    @MutationMapping
    public Post upvotePost(@Argument final UUID postId,
                        @ContextValue final LoggedInUser currentUser) {
        PostEntity post = postRepository.findById(postId).orElseThrow(() ->
                new EntityNotFoundException("Post with the id"  + postId + "not found"));
        ThreadEntity thread = threadRepository.findById(post.getThread().getId()).orElseThrow(()->
                new EntityNotFoundException("Thread with the id"  + post.getThread().getId() + "not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, thread.getForum().getCourseId());

        return forumService.upvotePost(post, currentUser.getId());
    }

    @MutationMapping
    public Post downvotePost(@Argument final UUID postId,
                           @ContextValue final LoggedInUser currentUser) {
        PostEntity post = postRepository.findById(postId).orElseThrow(() ->
                new EntityNotFoundException("Post with the id"  + postId + "not found"));
        ThreadEntity thread = threadRepository.findById(post.getThread().getId()).orElseThrow(()->
                new EntityNotFoundException("Thread with the id"  + post.getThread().getId() + "not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, thread.getForum().getCourseId());

        return forumService.downvotePost(post, currentUser.getId());
    }

    @MutationMapping
    public ThreadMediaRecordReference addThreadToMediaRecord(@Argument final InputThreadMediaRecordReference inputThreadMediaRecordReference,
                                                             @ContextValue final LoggedInUser currentUser) {
        ThreadEntity thread = threadRepository.findById(inputThreadMediaRecordReference.getThreadId()).orElseThrow(()->
                new EntityNotFoundException("Thread with the id"  + inputThreadMediaRecordReference.getThreadId() + "not found"));
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, thread.getForum().getCourseId());
        MediaRecordEntity mediaRecord = mediaRecordRepository.findById(inputThreadMediaRecordReference.getMediaRecordId()).orElseThrow(()->
                new EntityNotFoundException("MediaRecord with the id"  + inputThreadMediaRecordReference.getMediaRecordId() + "not found"));
        return forumService.addThreadToMediaRecord(thread, mediaRecord, inputThreadMediaRecordReference.getTimeStampSeconds(), inputThreadMediaRecordReference.getPageNumber());
    }
}
