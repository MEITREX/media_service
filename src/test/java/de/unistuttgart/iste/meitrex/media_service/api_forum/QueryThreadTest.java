package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Forum;
import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import de.unistuttgart.iste.meitrex.media_service.test_util.MediaRecordRepositoryUtil;
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

import static graphql.ErrorType.DataFetchingException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;

@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
public class QueryThreadTest {
    @Autowired
    private ForumRepository forumRepository;

    private final ModelMapper modelMapper = new ModelMapper();

    private final UUID courseId1 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.SUPER_USER), courseMembership1);
    @Autowired
    private MediaRecordRepository mediaRecordRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private ThreadMediaRecordReferenceRepository threadMediaRecordReferenceRepository;
    @Autowired
    private ForumMapper forumMapper;
    @Autowired
    private ThreadMapper threadMapper;

    @Test
    void testQueryThreadEmpty(final GraphQlTester tester) {
        UUID threadId = UUID.randomUUID();
        final String query = """
                query {
                    thread(id: "%s") {
                        id
                    }
                }
                """.formatted(threadId);
        tester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.getFirst().getMessage(), containsString("Thread with the id " + threadId + " not found"));
                    assertThat(errors.getFirst().getErrorType(), is(DataFetchingException));
                });
        assertThat(threadRepository.findAll(), hasSize(0));
    }

    @Test
    void testQueryQuestionThread(final GraphQlTester tester) {
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
                .numberOfPosts(0)
                .build();
        questionEntity.setThread(threadEntity);
        questionEntity = postRepository.save(questionEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        final String query = """
                query {
                    thread(id: "%s") {
                        ... on QuestionThread {
                            id
                            creatorId
                            creationTime
                            posts {
                                id
                            }
                            title
                            question {
                                authorId
                                content
                                creationTime
                            }
                        }
                    }
                }
                """.formatted(threadEntity.getId());
        QuestionThread thread = tester.document(query)
                .execute()
                .path("thread").entity(QuestionThread.class).get();
        assertThat(thread.getId(), is(threadEntity.getId()));
        assertThat(thread.getCreatorId(), is(thread.getCreatorId()));
        assertThat(thread.getTitle(), is(threadEntity.getTitle()));
        assertThat(thread.getQuestion().getContent(), is(threadEntity.getQuestion().getContent()));
        assertThat(thread.getQuestion().getAuthorId(), is(threadEntity.getQuestion().getAuthorId()));
        assertThat(forumRepository.findAll(), hasSize(1));
    }

    @Test
    void testQueryInfoThread(final GraphQlTester tester) {
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
                .threadMediaRecordReference(null)
                .posts(new ArrayList<>())
                .creatorId(currentUser.getId())
                .creationTime(OffsetDateTime.now())
                .numberOfPosts(0)
                .build();
        infoEntity.setThread(threadEntity);
        infoEntity = postRepository.save(infoEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        final String query = """
                query {
                    thread(id: "%s") {
                        ... on InfoThread {
                            id
                            creatorId
                            creationTime
                            posts {
                                id
                            }
                            title
                            info {
                                authorId
                                content
                                creationTime
                            }
                        }
                    }
                }
                """.formatted(threadEntity.getId());
        InfoThread thread = tester.document(query)
                .execute()
                .path("thread").entity(InfoThread.class).get();
        assertThat(thread.getId(), is(threadEntity.getId()));
        assertThat(thread.getCreatorId(), is(thread.getCreatorId()));
        assertThat(thread.getTitle(), is(threadEntity.getTitle()));
        assertThat(thread.getInfo().getContent(), is(threadEntity.getInfo().getContent()));
        assertThat(thread.getInfo().getAuthorId(), is(threadEntity.getInfo().getAuthorId()));
        assertThat(forumRepository.findAll(), hasSize(1));
    }
}
