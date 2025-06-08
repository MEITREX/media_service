package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.service.ForumService;
import graphql.schema.DataFetchingEnvironment;
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
    public QuestionThread createQuestionThread(@Argument final QuestionThread questionThread,
                               @ContextValue final LoggedInUser currentUser) {
        if (questionThread.getForum().getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, questionThread.getForum().getCourseId());
        }
        return forumService.createQuestionThread(questionThread);
    }

    @MutationMapping
    public InfoThread createInfoThread(@Argument final InfoThread infoThread,
                                           @ContextValue final LoggedInUser currentUser) {
        if (infoThread.getForum().getCourseId() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, infoThread.getForum().getCourseId());
        }
        return forumService.createInfoThread(infoThread);
    }

    @MutationMapping
    public Post addPost(@Argument final Post post,
                        @ContextValue final LoggedInUser currentUser) {
        if (post.getThread() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, post.getThread().getForum().getCourseId());
        }
        return forumService.addPostToThread(post);
    }

    @MutationMapping
    public Post upvotePost(@Argument final Post post,
                        @ContextValue final LoggedInUser currentUser) {
        if (post.getThread() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, post.getThread().getForum().getCourseId());
        }
        return forumService.upvotePost(post, currentUser.getId());
    }

    @MutationMapping
    public Post downvotePost(@Argument final Post post,
                           @ContextValue final LoggedInUser currentUser) {
        if (post.getThread() != null) {
            validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.ADMINISTRATOR, post.getThread().getForum().getCourseId());
        }
        return forumService.downvotePost(post, currentUser.getId());
    }
}
