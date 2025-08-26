package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ForumActivity;
import de.unistuttgart.iste.meitrex.common.event.ForumActivityEvent;
import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.testutil.MockTestPublisherConfiguration;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.InfoThread;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = {MockTestPublisherConfiguration.class})
@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
class MutationCreateInfoThreadTest {
    @Autowired
    private ForumRepository forumRepository;

    private final UUID courseId1 = UUID.randomUUID();

    private final LoggedInUser.CourseMembership courseMembership1 = CourseMembershipUtil.dummyCourseMembershipBuilder(courseId1);

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipsAndRealmRoles(Set.of(LoggedInUser.RealmRole.SUPER_USER), courseMembership1);
    @Autowired
    private ThreadRepository threadRepository;
    @Autowired
    private MediaRecordRepository mediaRecordRepository;
    @Autowired
    private TopicPublisher topicPublisher;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ThreadContentReferenceRepository threadContentReferenceRepository;


    @Test
    void testAddInfoThreadToForum(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(isA(ForumActivityEvent.class));

        final String query = """
                mutation {
                    createInfoThread(
                        thread: {forumId: "%s", title: "Test title", info: {content: "Test Info"}}
                    ) {
                        id
                        title
                        info {
                            content
                        }
                    }
                }
                """.formatted(forumEntity.getId());
        InfoThread infoThread = tester.document(query)
                .execute()
                .path("createInfoThread").entity(InfoThread.class).get();
        assertThat(infoThread.getTitle(), is("Test title"));
        assertThat(infoThread.getInfo().getContent(), is("Test Info"));
        assertThat(threadRepository.findAll(), hasSize(1));
        assertThat(forumRepository.findAll(), hasSize(1));
        assertThat(postRepository.findAll(), hasSize(1));
        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.THREAD));
    }

    @Test
    void testAddInfoThreadToForumAndContent(final GraphQlTester tester) {
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
                    createInfoThread(
                        thread: {forumId: "%s", title: "Test title", info: {content: "Test Info"}, threadContentReference: {contentId: "%s", timeStampSeconds: 10, pageNumber: 20}}
                    ) {
                        id
                        title
                        info {
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
        InfoThread infoThread = tester.document(query)
                .execute()
                .path("createInfoThread").entity(InfoThread.class).get();
        assertThat(infoThread.getTitle(), is("Test title"));
        assertThat(infoThread.getInfo().getContent(), is("Test Info"));
        assertThat(infoThread.getThreadContentReference().getContentId(), is(contentId));
        assertThat(infoThread.getThreadContentReference().getThreadId(), is(infoThread.getId()));
        assertThat(infoThread.getThreadContentReference().getTimeStampSeconds(), is(10));
        assertThat(infoThread.getThreadContentReference().getPageNumber(), is(20));
        assertThat(threadRepository.findAll(), hasSize(1));
        assertThat(forumRepository.findAll(), hasSize(1));
        assertThat(threadContentReferenceRepository.findAll(), hasSize(1));
        assertThat(postRepository.findAll(), hasSize(1));

        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.THREAD));
    }

    @Test
    void testAddInfoThreadWithProfanityToForum(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);

        doNothing().when(topicPublisher).notifyForumActivity(isA(ForumActivityEvent.class));

        final String query = """
                mutation {
                    createInfoThread(
                        thread: {forumId: "%s", title: "Die Frage ist scheiße!", info: {content: "Du bist scheiße!"}}
                    ) {
                        id
                        title
                        info {
                            content
                        }
                    }
                }
                """.formatted(forumEntity.getId());
        InfoThread infoThread = tester.document(query)
                .execute()
                .path("createInfoThread").entity(InfoThread.class).get();
        assertThat(infoThread.getTitle(), is("Die Frage ist *******!"));
        assertThat(infoThread.getInfo().getContent(), is("Du bist *******!"));
        assertThat(threadRepository.findAll(), hasSize(1));
        assertThat(forumRepository.findAll(), hasSize(1));
        assertThat(postRepository.findAll(), hasSize(1));
        verify(topicPublisher).notifyForumActivity(new ForumActivityEvent(currentUser.getId(), forumEntity.getId(),
                courseId1, ForumActivity.THREAD));
    }
}
