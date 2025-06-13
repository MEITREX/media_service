package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.ThreadMediaRecordReference;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
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

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;
import static graphql.ErrorType.DataFetchingException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
class MutationAddThreadToMediaRecordTest {
    @Autowired
    private ForumRepository forumRepository;

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

    @Test
    void testAddThreadToMediaRecordWrongRecordId(final GraphQlTester tester) {
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
        postRepository.save(questionEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        UUID mediaRecordId = UUID.randomUUID();
        final String query = """
                mutation {
                    addThreadToMediaRecord(
                        threadMediaRecordReference: {threadId: "%s", mediaRecordId: "%s"}
                    ) {
                        threadId
                        mediaRecordId
                    }
                }
                """.formatted(threadEntity.getId(), mediaRecordId);
        tester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.getFirst().getMessage(), containsString("MediaRecord with the id " + mediaRecordId + " not found"));
                    assertThat(errors.getFirst().getErrorType(), is(DataFetchingException));
                });
        assertThat(threadMediaRecordReferenceRepository.findAll(), hasSize(0));
    }

    @Test
    void testAddThreadToMediaRecordWrongThreadId(final GraphQlTester tester) {
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
        postRepository.save(questionEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        MediaRecordEntity mediaRecord = MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds(mediaRecordRepository, courseId1, UUID.randomUUID()).getFirst();
        UUID mediaRecordId = mediaRecord.getId();
        UUID threadId = UUID.randomUUID();
        final String query = """
                mutation {
                    addThreadToMediaRecord(
                        threadMediaRecordReference: {threadId: "%s", mediaRecordId: "%s"}
                    ) {
                        threadId
                        mediaRecordId
                    }
                }
                """.formatted(threadId, mediaRecordId);
        tester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.getFirst().getMessage(), containsString("Thread with id " + threadId + " not found"));
                    assertThat(errors.getFirst().getErrorType(), is(DataFetchingException));
                });
        assertThat(threadMediaRecordReferenceRepository.findAll(), hasSize(0));
    }

    @Test
    void testAddThreadToMediaRecord(final GraphQlTester tester) {
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
        postRepository.save(questionEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        MediaRecordEntity mediaRecord = MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds(mediaRecordRepository, courseId1, UUID.randomUUID()).getFirst();
        UUID mediaRecordId = mediaRecord.getId();
        final String query = """
                mutation {
                    addThreadToMediaRecord(
                        threadMediaRecordReference: {threadId: "%s", mediaRecordId: "%s"}
                    ) {
                        threadId
                        mediaRecordId
                    }
                }
                """.formatted(threadEntity.getId(), mediaRecordId);
        ThreadMediaRecordReference threadMediaRecordReference = tester.document(query)
                .execute()
                .path("addThreadToMediaRecord").entity(ThreadMediaRecordReference.class).get();
        assertThat(threadMediaRecordReference.getThreadId(), is(threadEntity.getId()));
        assertThat(threadMediaRecordReference.getMediaRecordId(), is(mediaRecordId));
        assertThat(threadMediaRecordReferenceRepository.findAll(), hasSize(1));
    }
}
