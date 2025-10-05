package de.unistuttgart.iste.meitrex.media_service.api_forum;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Post;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ForumEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.QuestionThreadEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.ForumRepository;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.MediaRecordRepository;
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
class MutationDeletePostTest {
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

    @Test
    void testDeletePostToThread(final GraphQlTester tester) {
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
        postRepository.save(questionEntity);
        threadEntity.getPosts().add(postEntity);
        threadRepository.save(threadEntity);
        forumEntity.getThreads().add(threadEntity);
        forumRepository.save(forumEntity);
        final String query = """
                mutation {
                    deletePost(
                        postId: "%s"
                    ) {
                        id
                        content
                    }
                }
                """.formatted(postEntity.getId());
        Post post = tester.document(query)
                .execute()
                .path("deletePost").entity(Post.class).get();
        assertThat(post.getContent(), is("Post Content"));
        assertThat(postRepository.findAll(), hasSize(1));
        assertThat(threadRepository.findAll(), hasSize(1));
    }
}
