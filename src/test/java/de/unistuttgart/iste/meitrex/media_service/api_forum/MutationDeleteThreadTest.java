package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.InfoThreadEntity;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
public class MutationDeleteThreadTest {
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

    @Test
    void testDeleteQuestionThread(final GraphQlTester tester) {
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
                .numberOfPosts(1)
                .build();
        PostEntity postEntity = PostEntity.builder()
                .content("Post Content")
                .authorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .thread(threadEntity)
                .build();
        postEntity = postRepository.save(postEntity);
        questionEntity.setThread(threadEntity);
        questionEntity = postRepository.save(questionEntity);
        threadEntity.getPosts().add(postEntity);
        threadEntity = threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumEntity = forumRepository.save(forumEntity);
        System.out.println(forumRepository.findAll());
        System.out.println(threadRepository.findAll());
        System.out.println(postRepository.findAll());
        final String query = """
                mutation {
                    deleteThread(
                        threadId: "%s"
                    ) {
                        id
                    }
                }
                """.formatted(threadEntity.getId());
        QuestionThread thread = tester.document(query)
                .execute()
                .path("deleteThread").entity(QuestionThread.class).get();
        assertThat(thread.getId(), is(threadEntity.getId()));
        assertThat(postRepository.findAll(), hasSize(0));
        assertThat(threadRepository.findAll(), hasSize(0));
        assertThat(forumRepository.findAll(), hasSize(1));
    }

    @Test
    void testDeleteInfoThread(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);
        PostEntity infoEntity = PostEntity.builder()
                .content("info Content")
                .authorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .build();
        InfoThreadEntity threadEntity = InfoThreadEntity.builder()
                .forum(forumEntity)
                .info(infoEntity)
                .title("Thread Title")
                .threadContentReferenceEntity(null)
                .posts(new ArrayList<>())
                .creatorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .numberOfPosts(1)
                .build();
        PostEntity postEntity = PostEntity.builder()
                .content("Post Content")
                .authorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .thread(threadEntity)
                .build();
        postEntity = postRepository.save(postEntity);
        infoEntity.setThread(threadEntity);
        infoEntity = postRepository.save(infoEntity);
        threadEntity.getPosts().add(postEntity);
        threadEntity = threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumEntity = forumRepository.save(forumEntity);
        System.out.println(forumRepository.findAll());
        System.out.println(threadRepository.findAll());
        System.out.println(postRepository.findAll());
        final String query = """
                mutation {
                    deleteThread(
                        threadId: "%s"
                    ) {
                        id
                    }
                }
                """.formatted(threadEntity.getId());
        InfoThread thread = tester.document(query)
                .execute()
                .path("deleteThread").entity(InfoThread.class).get();
        assertThat(thread.getId(), is(threadEntity.getId()));
        assertThat(postRepository.findAll(), hasSize(0));
        assertThat(threadRepository.findAll(), hasSize(0));
        assertThat(forumRepository.findAll(), hasSize(1));
    }
}
