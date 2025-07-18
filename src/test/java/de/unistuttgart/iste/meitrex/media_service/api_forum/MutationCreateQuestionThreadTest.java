package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ForumActivity;
import de.unistuttgart.iste.meitrex.common.event.ForumActivityEvent;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionThread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import de.unistuttgart.iste.meitrex.media_service.test_util.MediaRecordRepositoryUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {MockTestPublisherConfiguration.class})
@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
class MutationCreateQuestionThreadTest {
    @Autowired
    private ForumRepository forumRepository;

    private final UUID courseId1 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.SUPER_USER), courseMembership1);

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private TopicPublisher topicPublisher;

    @Autowired
    private MediaRecordRepository mediaRecordRepository;
    @Autowired
    private ThreadContentReferenceRepository threadContentReferenceRepository;
    @Autowired
    private PostRepository postRepository;

    @Test
    void testAddQuestionThreadToForum(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(isA(ForumActivityEvent.class));

        final String query = """
                mutation {
                    createQuestionThread(
                        thread: {forumId: "%s", title: "Test title", question: {content: "Test Question"}}
                    ) {
                        id
                        title
                        question {
                            content
                        }
                    }
                }
                """.formatted(forumEntity.getId());

        QuestionThread questionThread = tester.document(query)
                .execute()
                .path("createQuestionThread").entity(QuestionThread.class).get();
        assertThat(questionThread.getTitle(), is("Test title"));
        assertThat(questionThread.getQuestion().getContent(), is("Test Question"));
        assertThat(threadRepository.findAll(), hasSize(1));
        assertThat(forumRepository.findAll(), hasSize(1));
        assertThat(postRepository.findAll(), hasSize(1));

        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.QUESTION));
    }

    @Test
    void testAddQuestionThreadToForumAndContent(final GraphQlTester tester) {
        MediaRecordEntity mediaRecord = MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds(mediaRecordRepository, courseId1, UUID.randomUUID()).getFirst();
        UUID contentId = mediaRecord.getContentIds().stream().findFirst().get();
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(isA(ForumActivityEvent.class));


        final String query = """
                mutation {
                    createQuestionThread(
                        thread: {forumId: "%s", title: "Test title", question: {content: "Test Question"}, threadContentReference: {contentId: "%s", timeStampSeconds: 10, pageNumber: 20}}
                    ) {
                        id
                        title
                        question {
                            content
                        }
                        threadContentReference {
                            contentId
                            threadId
                            timeStampSeconds
                            pageNumber
                        }
                    }
                }
                """.formatted(forumEntity.getId(), contentId);
        QuestionThread questionThread = tester.document(query)
                .execute()
                .path("createQuestionThread").entity(QuestionThread.class).get();
        assertThat(questionThread.getTitle(), is("Test title"));
        assertThat(questionThread.getQuestion().getContent(), is("Test Question"));
        assertThat(questionThread.getThreadContentReference().getContentId(), is(contentId));
        assertThat(questionThread.getThreadContentReference().getThreadId(), is(questionThread.getId()));
        assertThat(questionThread.getThreadContentReference().getTimeStampSeconds(), is(10));
        assertThat(questionThread.getThreadContentReference().getPageNumber(), is(20));
        assertThat(threadRepository.findAll(), hasSize(1));
        assertThat(threadContentReferenceRepository.findAll(), hasSize(1));
        assertThat(forumRepository.findAll(), hasSize(1));
        assertThat(postRepository.findAll(), hasSize(1));

        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.QUESTION));
    }
}
