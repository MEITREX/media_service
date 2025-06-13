package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Forum;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import de.unistuttgart.iste.meitrex.media_service.test_util.CourseMembershipUtil;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

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
public class QueryForumByCourseIdTest {
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

    @Test
    void testQueryForumEmpty(final GraphQlTester tester) {
        final String query = """
                query {
                    forumByCourseId(id: "%s") {
                        id
                        courseId
                        threads {
                            id
                        }
                    }
                }
                """.formatted(courseId1);
        Forum forum = tester.document(query)
                .execute()
                .path("forumByCourseId").entity(Forum.class).get();
        assertThat(forum.getCourseId(), is(courseId1));
        assertThat(forumRepository.findAll(), hasSize(1));
    }

    @Test
    void testQueryForum(final GraphQlTester tester) {
        ForumEntity forumEntity = ForumEntity.builder()
                .courseId(courseId1)
                .threads(new ArrayList<>())
                .build();
        forumEntity = forumRepository.save(forumEntity);
        final String query = """
                query {
                    forumByCourseId(id: "%s") {
                        id
                        courseId
                        threads {
                            id
                        }
                    }
                }
                """.formatted(forumEntity.getCourseId());
        Forum forum = tester.document(query)
                .execute()
                .path("forumByCourseId").entity(Forum.class).get();
        assertThat(modelMapper.map(forum, ForumEntity.class), is(forumEntity));
        assertThat(forumRepository.findAll(), hasSize(1));
    }
}
