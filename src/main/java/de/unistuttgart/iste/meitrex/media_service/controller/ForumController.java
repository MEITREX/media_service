package de.unistuttgart.iste.meitrex.media_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Forum;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.service.ForumService;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.GlobalPermissionAccessValidator.validateUserHasGlobalPermission;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ForumController {
    private final ForumService forumService;

    @QueryMapping
    public Forum getForum(@Argument final UUID id,
                              @ContextValue final LoggedInUser currentUser) {
        Forum forum = forumService.getForumById(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,forum.getCourseId());
        return forum;
    }

    @QueryMapping
    public Thread getThread(@Argument final UUID id,
                            @ContextValue final LoggedInUser currentUser) {
        Thread thread = forumService.getThreadById(id);
        validateUserHasAccessToCourse(currentUser, LoggedInUser.UserRoleInCourse.STUDENT ,thread.getForum().getCourseId());
        return thread;
    }
}
