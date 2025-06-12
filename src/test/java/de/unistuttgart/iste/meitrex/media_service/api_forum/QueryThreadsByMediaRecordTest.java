package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Forum;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static graphql.ErrorType.DataFetchingException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipsAndRealmRoles;

@GraphQlApiTest
@Transactional
@ActiveProfiles("test")
public class QueryThreadsByMediaRecordTest {
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
        MediaRecordEntity mediaRecord = MediaRecordRepositoryUtil.fillRepositoryWithMediaRecords(mediaRecordRepository).getFirst();

        UUID courseId = mediaRecord.getCourseIds().getFirst();
        UUID mediaRecordId = mediaRecord.getId();

        final String query = """
                query {
                    threadsByMediaRecord(id: "%s") {
                        id
                        title
                        creationTime
                        creatorId
                    }
                }
                """.formatted(mediaRecordId);
        List<Thread> threadList = tester.document(query)
                .execute()
                .path("threadsByMediaRecord")
                .entityList(Thread.class).get();

        assertThat(threadRepository.findAll(), hasSize(0));
    }

    @Test
    void testQueryThread(final GraphQlTester tester) {
        MediaRecordEntity mediaRecord = MediaRecordRepositoryUtil.fillRepositoryWithMediaRecordsAndCourseIds(mediaRecordRepository, courseId1, UUID.randomUUID()).getFirst();
        UUID mediaRecordId = mediaRecord.getId();
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
                .build();
        questionEntity.setThread(threadEntity);
        postRepository.save(questionEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        ThreadMediaRecordReferenceEntity threadMediaRecordReference = new ThreadMediaRecordReferenceEntity(threadEntity,
                mediaRecord, null, null);
        threadMediaRecordReferenceRepository.save(threadMediaRecordReference);
        threadEntity.setThreadMediaRecordReference(threadMediaRecordReference);
        threadRepository.save(threadEntity);

        final String query = """
                query {
                    threadsByMediaRecord(id: "%s") {
                        id
                        title
                        creationTime
                        creatorId
                    }
                }
                """.formatted(mediaRecordId);
        List<QuestionThread> threadList = tester.document(query)
                .execute()
                .path("threadsByMediaRecord")
                .entityList(QuestionThread.class).get();

        assertThat(threadList, hasSize(1));
        assertThat(threadList.getFirst().getId(), is(threadEntity.getId()));
        assertThat(threadList.getFirst().getTitle(), is(threadEntity.getTitle()));
        assertThat(threadList.getFirst().getCreatorId(), is(threadEntity.getCreatorId()));
        assertThat(threadRepository.findAll(), hasSize(1));
    }
}
