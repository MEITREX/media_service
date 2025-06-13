package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.InfoThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static graphql.ErrorType.DataFetchingException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
class MutationSelectAnswerTest {

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
    void testSelectAnswerWrongPostId(final GraphQlTester tester) {
        UUID postId = UUID.randomUUID();
        final String query = """
                mutation {
                    selectAnswer(
                        postId: "%s"
                    ) {
                        id
                        selectedAnswer {
                            id
                            content
                            creationTime
                            authorId
                        }
                    }
                }
                """.formatted(postId);
        tester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.getFirst().getMessage(), containsString("Post with the id " +
                            postId + " not found"));
                    assertThat(errors.getFirst().getErrorType(), is(DataFetchingException));
                });
    }

    @Test
    void testSelectAnswerInfoThread(final GraphQlTester tester) {
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
        InfoThreadEntity threadEntity = InfoThreadEntity.builder()
                .forum(forumEntity)
                .info(questionEntity)
                .title("Thread Title")
                .threadMediaRecordReference(null)
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
        postRepository.save(questionEntity);
        threadEntity.getPosts().add(postEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        final String query = """
                mutation {
                    selectAnswer(
                        postId: "%s"
                    ) {
                        id
                        selectedAnswer {
                            id
                            content
                            creationTime
                            authorId
                        }
                    }
                }
                """.formatted(postEntity.getId());
        tester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.getFirst().getMessage(), containsString("Thread with the id " +
                            threadEntity.getId() + " is not a questionThread"));
                    assertThat(errors.getFirst().getErrorType(), is(DataFetchingException));
                });
    }

    @Test
    void testSelectAnswer(final GraphQlTester tester) {
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
                .threadMediaRecordReference(null)
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
        postRepository.save(questionEntity);
        threadEntity.getPosts().add(postEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        final String query = """
                mutation {
                    selectAnswer(
                        postId: "%s"
                    ) {
                        id
                        selectedAnswer {
                            id
                            content
                            creationTime
                            authorId
                        }
                    }
                }
                """.formatted(postEntity.getId());
        QuestionThread questionThread = tester.document(query)
                .execute()
                .path("selectAnswer").entity(QuestionThread.class).get();
        assertThat(questionThread.getSelectedAnswer().getContent(), is(postEntity.getContent()));
        assertThat(questionThread.getSelectedAnswer().getAuthorId(), is(postEntity.getAuthorId()));
        assertThat(questionThread.getSelectedAnswer().getId(), is(postEntity.getId()));
        assertThat(postRepository.findAll(), hasSize(2));
    }
}
