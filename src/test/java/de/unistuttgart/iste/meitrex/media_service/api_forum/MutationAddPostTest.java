package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ForumActivity;
import de.unistuttgart.iste.meitrex.common.event.ForumActivityEvent;
import de.unistuttgart.iste.meitrex.common.event.MediaRecordDeletedEvent;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Post;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ForumRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.PostRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ThreadRepository;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = {MockTestPublisherConfiguration.class})
@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
class MutationAddPostTest {
    @Autowired
    private ForumRepository forumRepository;

    private final UUID courseId1 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.SUPER_USER), courseMembership1);
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private TopicPublisher topicPublisher;

    @Test
    void testAddPostToThread(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);
        PostEntity questionEntity = PostEntity.builder()
                .content("question Content")
                .authorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .build();
        QuestionThreadEntity threadEntity = QuestionThreadEntity.builder()
                .forum(forumEntity)
                .question(questionEntity)
                .title("Thread Title")
                .threadContentReferenceEntity(null)
                .posts(new ArrayList<>())
                .creatorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .numberOfPosts(0)
                .build();
        questionEntity.setThread(threadEntity);
        postRepository.save(questionEntity);
        threadEntity = threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.ANSWER));

        final String query = """
                mutation {
                    addPost(
                        post: {content: "Test Content", threadId: "%s"}
                    ) {
                        id
                        content
                        edited
                    }
                }
                """.formatted(threadEntity.getId());
        Post post = tester.document(query)
                .execute()
                .path("addPost").entity(Post.class).get();

        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.ANSWER));

        assertThat(post.getContent(), is("Test Content"));
        assertThat(post.getEdited(), is(false));
        assertThat(postRepository.findAll(), hasSize(2));
        assertThat(threadRepository.findAll(), hasSize(1));
    }

    @Test
    void testAddPostWithProfanityToThread(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);
        PostEntity questionEntity = PostEntity.builder()
                .content("question Content")
                .authorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .build();
        QuestionThreadEntity threadEntity = QuestionThreadEntity.builder()
                .forum(forumEntity)
                .question(questionEntity)
                .title("Thread Title")
                .threadContentReferenceEntity(null)
                .posts(new ArrayList<>())
                .creatorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .numberOfPosts(0)
                .build();
        questionEntity.setThread(threadEntity);
        postRepository.save(questionEntity);
        threadEntity = threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.ANSWER));

        final String query = """
                mutation {
                    addPost(
                        post: {content: "<p>Du bist ein Arschloch!</p>", threadId: "%s"}
                    ) {
                        id
                        content
                        edited
                    }
                }
                """.formatted(threadEntity.getId());
        Post post = tester.document(query)
                .execute()
                .path("addPost").entity(Post.class).get();

        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.ANSWER));

        assertThat(post.getContent(), is("<p>Du bist ein *********!</p>"));
        assertThat(post.getEdited(), is(false));
        assertThat(postRepository.findAll(), hasSize(2));
        assertThat(threadRepository.findAll(), hasSize(1));
    }
}
