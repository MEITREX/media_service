package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.profanity_filter.ProfanityFilter;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import javax.naming.AuthenticationException;
import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForumServiceTest {
    private final ForumRepository forumRepository = mock(ForumRepository.class);
    private final ThreadRepository threadRepository = mock(ThreadRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final ThreadContentReferenceRepository threadContentReferenceRepository = mock(ThreadContentReferenceRepository.class);
    private final MediaRecordRepository mediaRecordRepository = mock(MediaRecordRepository.class);

    private final TopicPublisher topicPublisher = mock(TopicPublisher.class);

    private final QuestionThreadRepository questionThreadRepository = mock(QuestionThreadRepository.class);

    private final ModelMapper modelMapper = new ModelMapper();
    private final PostMapper postMapper = new PostMapper();
    private final QuestionThreadMapper questionThreadMapper = new QuestionThreadMapper(postMapper, modelMapper);
    private final InfoThreadMapper  infoThreadMapper = new InfoThreadMapper(postMapper, modelMapper);
    private final ThreadMapper threadMapper = new ThreadMapper(infoThreadMapper, questionThreadMapper);
    private final ForumMapper forumMapper = new ForumMapper(threadMapper);
    private final ProfanityFilter profanityFilter = mock(ProfanityFilter.class);

    private final ForumService forumService = new ForumService(modelMapper, forumRepository, threadRepository,
            postRepository, threadContentReferenceRepository, mediaRecordRepository, topicPublisher, profanityFilter,
            forumMapper, threadMapper,postMapper, questionThreadMapper, infoThreadMapper, questionThreadRepository);

    @Test
    void testGetForumById() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();
        when(forumRepository.findById(forum.getId())).thenReturn(Optional.of(forum));

        assertThat(modelMapper.map(forumService.getForumById(forum.getId()), ForumEntity.class), is(forum));

        final UUID notExistingId = UUID.randomUUID();
        assertThrows(EntityNotFoundException.class, () -> forumService.getForumById(notExistingId));
    }

    @Test
    void testGetForumByCourseId() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();
        when(forumRepository.findByCourseId(forum.getCourseId())).thenReturn(Optional.of(forum));

        assertThat(modelMapper.map(forumService.getForumByCourseId(forum.getCourseId()), ForumEntity.class), is(forum));

        final UUID notExistingId = UUID.randomUUID();
        ForumEntity forumNotExists = new ForumEntity(notExistingId);
        when(forumRepository.save(forumNotExists)).thenReturn(forumNotExists);

        assertThat(modelMapper.map(forumService.getForumByCourseId(notExistingId), ForumEntity.class), is(forumNotExists));
    }

    @Test
    void testGetThreadById() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();
        final ThreadEntity thread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(List.of())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        when(threadRepository.findById(thread.getId())).thenReturn(Optional.of(thread));

        assertThat(modelMapper.map(forumService.getThreadById(thread.getId()), ThreadEntity.class), is(thread));

        final UUID notExistingId = UUID.randomUUID();
        assertThrows(EntityNotFoundException.class, () -> forumService.getThreadById(notExistingId));
    }

    @Test
    void testGetThreadsByMediaRecord() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();
        final PostEntity question = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestQuestion")
                .authorId(UUID.randomUUID())
                .build();
        final QuestionThreadEntity thread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("TestTitle")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .forum(forum)
                .question(question)
                .numberOfPosts(0)
                .build();
        UUID contentId = UUID.randomUUID();
        final ThreadContentReferenceEntity threadContentReference = new ThreadContentReferenceEntity(thread, contentId, null, null);

        thread.setThreadContentReference(threadContentReference);
        when(threadContentReferenceRepository.findAllByContentId(contentId)).thenReturn(List.of(threadContentReference));
        assertThat(forumService.getThreadsByThreadContentReferences(List.of(threadContentReference)).getFirst(),
                is(threadMapper.mapThread(thread)));
    }

    @Test
    void testAddPostToThread() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        final PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .build();
        InputPost post = InputPost.builder()
                .setContent(postEntity.getContent())
                .build();
        final PostEntity pPostEntity = new PostEntity(post.getContent(), postEntity.getAuthorId(), threadEntity);
        final PostEntity returnPostEntity = new PostEntity(post.getContent(), postEntity.getAuthorId(), threadEntity);
        returnPostEntity.setId(UUID.randomUUID());
        returnPostEntity.setCreationTime(OffsetDateTime.now());

        final PostEntity finalPostEntity = new PostEntity(post.getContent(), postEntity.getAuthorId(), threadEntity);
        finalPostEntity.setId(returnPostEntity.getId());
        finalPostEntity.setCreationTime(returnPostEntity.getCreationTime());
        finalPostEntity.setThread(null);

        when(postRepository.save(pPostEntity)).thenReturn(returnPostEntity);
        when(threadRepository.save(threadEntity)).thenReturn(threadEntity);
        when(profanityFilter.censor(postEntity.getContent())).thenReturn(postEntity.getContent());

        assertThat(modelMapper.map(forumService.addPostToThread(post,threadEntity,postEntity.getAuthorId()), PostEntity.class), is(finalPostEntity));
    }

    @Test
    void testUpvotePost() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        final PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        when(postRepository.save(Mockito.any(PostEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        UUID userId = UUID.randomUUID();

        final PostEntity resultPostEntity = PostEntity.builder()
                .id(postEntity.getId())
                .content(postEntity.getContent())
                .authorId(postEntity.getAuthorId())
                .thread(postEntity.getThread())
                .downvotedByUsers(new ArrayList<>())
                .upvotedByUsers(List.of(userId))
                .build();

        assertThat(forumService.upvotePost(postEntity, userId), is(modelMapper.map(resultPostEntity, Post.class)));
    }

    @Test
    void testDownvotePost() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        final PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        when(postRepository.save(Mockito.any(PostEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        UUID userId = UUID.randomUUID();

        final PostEntity resultPostEntity = PostEntity.builder()
                .id(postEntity.getId())
                .content(postEntity.getContent())
                .authorId(postEntity.getAuthorId())
                .thread(postEntity.getThread())
                .downvotedByUsers(List.of(userId))
                .upvotedByUsers(new ArrayList<>())
                .build();

        assertThat(forumService.downvotePost(postEntity, userId), is(modelMapper.map(resultPostEntity, Post.class)));
    }

    @Test
    void testCreateQuestionThread() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(new ArrayList<>())
                .userIds(Set.of())
                .build();

        final QuestionThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("TestTitle")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .forum(forum)
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();

        final PostEntity questionEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        final InputPost question = InputPost.builder()
                .setContent(questionEntity.getContent())
                .build();

        final InputQuestionThread inputQuestionThread = InputQuestionThread.builder()
                .setQuestion(question)
                .setTitle(threadEntity.getTitle())
                .build();

        final PostEntity inputPostEntity = new PostEntity(inputQuestionThread.getQuestion().getContent(),
                threadEntity.getCreatorId());

        final QuestionThreadEntity questionThreadEntity = new QuestionThreadEntity(forum, threadEntity.getCreatorId(),
                threadEntity.getTitle(), inputPostEntity);
        inputPostEntity.setThread(questionThreadEntity);

        threadEntity.setQuestion(inputPostEntity);

        when(postRepository.save(inputPostEntity)).thenReturn(inputPostEntity);
        when(forumRepository.save(Mockito.any(ForumEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(threadRepository.save(Mockito.any(ThreadEntity.class))).thenReturn(threadEntity);
        assertThat(forumService.createQuestionThread(inputQuestionThread, forum, threadEntity.getCreatorId()),
                is(modelMapper.map(threadEntity, QuestionThread.class)));
    }

    @Test
    void testCreateInfoThread() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(new ArrayList<>())
                .userIds(Set.of())
                .build();

        final InfoThreadEntity threadEntity = InfoThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("TestTitle")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .forum(forum)
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();

        final PostEntity infoEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        final InputPost info = InputPost.builder()
                .setContent(infoEntity.getContent())
                .build();

        final InputInfoThread inputInfoThread = InputInfoThread.builder()
                .setInfo(info)
                .setTitle(threadEntity.getTitle())
                .build();

        final PostEntity inputPostEntity = new PostEntity(inputInfoThread.getInfo().getContent(),
                threadEntity.getCreatorId());

        final InfoThreadEntity infoThreadEntity = new InfoThreadEntity(forum, threadEntity.getCreatorId(),
                threadEntity.getTitle(), inputPostEntity);
        inputPostEntity.setThread(infoThreadEntity);

        threadEntity.setInfo(inputPostEntity);

        when(postRepository.save(inputPostEntity)).thenReturn(inputPostEntity);
        when(forumRepository.save(Mockito.any(ForumEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        when(threadRepository.save(Mockito.any(ThreadEntity.class))).thenReturn(threadEntity);
        assertThat(forumService.createInfoThread(inputInfoThread, forum, threadEntity.getCreatorId()),
                is(modelMapper.map(threadEntity, InfoThread.class)));
    }

    @Test
    void testUpdatePost() throws AuthenticationException {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        final PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .build();
        InputPost post = InputPost.builder()
                .setContent(postEntity.getContent())
                .build();
        when(postRepository.save(Mockito.any(PostEntity.class))).thenReturn(postEntity);
        assertThat(forumService.updatePost(post, postEntity, postEntity.getAuthorId()), is(modelMapper.map(postEntity, Post.class)));
    }

    @Test
    void testDeletePost() throws AuthenticationException {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();
        final PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("TestPost")
                .authorId(UUID.randomUUID())
                .thread(threadEntity)
                .build();
        LoggedInUser loggedInUser = LoggedInUser.builder()
                .id(postEntity.getAuthorId())
                .build();
        assertThat(forumService.deletePost(postEntity, loggedInUser), is(modelMapper.map(postEntity, Post.class)));
    }

    @Test
    void testAddThreadToMediaRecord() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .userIds(Set.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadContentReferenceEntity(null)
                .numberOfPosts(0)
                .build();


        final int timeStamp = 10;
        final int pageNumber = 3;

        UUID contentId = UUID.randomUUID();

        final ThreadContentReferenceEntity threadContentReferenceEntity = new
                ThreadContentReferenceEntity(threadEntity, contentId, timeStamp, pageNumber);

        assertThat(forumService.addThreadToContent(threadEntity, contentId, timeStamp, pageNumber),
                is(modelMapper.map(threadContentReferenceEntity, ThreadContentReference.class)));
    }

    @Test
    void testForumActivity_withPostsAndThreads() {
        // Arrange
        UUID creatorId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        // Create PostEntity
        PostEntity postEntity = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("Post content")
                .authorId(authorId)
                .creationTime(now.minusHours(5))
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        // Create ThreadEntity
        QuestionThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("Thread Title")
                .creatorId(creatorId)
                .creationTime(now.minusDays(1))
                .posts(List.of(postEntity))
                .numberOfPosts(1)
                .build();

        // Link post to thread
        postEntity.setThread(threadEntity);

        // Create ForumEntity
        ForumEntity forumEntity = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of(threadEntity))
                .userIds(Set.of(creatorId, authorId))
                .build();

        threadEntity.setForum(forumEntity);

        // Convert to DTO using forumMapper
        Forum forum = forumMapper.forumEntityToForum(forumEntity);

        // Act
        List<ForumActivityEntry> activities = forumService.forumActivity(forum);

        // Assert
        assertThat(activities.size(), is(2));

        // First entry must be the post (newer)
        assertThat(activities.get(0).getPost().getContent(), is("Post content"));
        assertThat(activities.get(0).getThread().getTitle(), is("Thread Title"));

        // Second entry is the thread
        assertThat(activities.get(1).getThread().getTitle(), is("Thread Title"));
        assertThat(activities.get(1).getPost(), is(nullValue()));
    }

    @Test
    void testForumActivityByUserId_returnsOnlyUserActivity() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        // Post own userId
        PostEntity userPost = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("User's Post")
                .authorId(userId)
                .creationTime(now.minusHours(1))
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        // Post of other user (should be ignored)
        PostEntity otherPost = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("Other User's Post")
                .authorId(otherUser)
                .creationTime(now.minusHours(2))
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        // Thread own userId
        QuestionThreadEntity userThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("User's Thread")
                .creatorId(userId)
                .creationTime(now.minusDays(1))
                .posts(List.of(userPost, otherPost))
                .numberOfPosts(2)
                .build();

        // Thread of other user (should be ignored)
        QuestionThreadEntity otherThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("Other's Thread")
                .creatorId(otherUser)
                .creationTime(now.minusDays(2))
                .posts(new ArrayList<>())
                .numberOfPosts(0)
                .build();

        // Forum
        ForumEntity forumEntity = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .userIds(Set.of(userId, otherUser))
                .threads(List.of(userThread, otherThread))
                .build();

        // Set parents
        userThread.setForum(forumEntity);
        otherThread.setForum(forumEntity);
        userPost.setThread(userThread);
        otherPost.setThread(userThread);

        // Mock Repository
        when(forumRepository.findAllByUserIdsContaining(userId))
                .thenReturn(List.of(forumEntity));

        // Act
        List<ForumActivityEntry> activities = forumService.forumActivityByUserId(userId);

        // Assert
        assertThat(activities.size(), is(2)); // userThread + userPost
        assertThat(activities.stream().allMatch(a ->
                a.getThread().getCreatorId().equals(userId)
                        || (a.getPost() != null && a.getPost().getAuthorId().equals(userId))
        ), is(true));

        // Newest first
        assertThat(activities.get(0).getCreationTime().isAfter(activities.get(1).getCreationTime()), is(true));
    }

    @Test
    void testOtherUserForumActivityByUserId_onlyCommonForumActivitiesIncluded() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        // Thread + Post from otherUser
        PostEntity otherPost = PostEntity.builder()
                .id(UUID.randomUUID())
                .content("Other's Post")
                .authorId(otherUserId)
                .creationTime(now.minusHours(3))
                .upvotedByUsers(new ArrayList<>())
                .downvotedByUsers(new ArrayList<>())
                .build();

        QuestionThreadEntity otherThread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("Other's Thread")
                .creatorId(otherUserId)
                .creationTime(now.minusDays(1))
                .posts(List.of(otherPost))
                .numberOfPosts(1)
                .build();

        // Common Forum
        ForumEntity commonForum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .userIds(Set.of(userId, otherUserId))
                .threads(List.of(otherThread))
                .build();

        // Set parent relationships
        otherThread.setForum(commonForum);
        otherPost.setThread(otherThread);

        // Forum only for otherUser (should be ignored)
        ForumEntity exclusiveForum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .userIds(Set.of(otherUserId))
                .threads(new ArrayList<>())
                .build();

        when(forumRepository.findAllByUserIdsContaining(userId)).thenReturn(List.of(commonForum));
        when(forumRepository.findAllByUserIdsContaining(otherUserId)).thenReturn(List.of(commonForum, exclusiveForum));

        // Act
        List<ForumActivityEntry> activities = forumService.otherUserForumActivityByUserId(userId, otherUserId);

        // Assert
        assertThat(activities.size(), is(2)); // Thread + Post
        assertThat(activities.stream().allMatch(a ->
                a.getThread().getCreatorId().equals(otherUserId)
                        || (a.getPost() != null && a.getPost().getAuthorId().equals(otherUserId))
        ), is(true));
    }

    // IDK how to test this
    @Test
    void testOpenQuestions_returnsTop4RankedQuestionsFromForumEntity() {
        OffsetDateTime now = OffsetDateTime.now();

        ForumEntity forumEntity = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .userIds(Set.of(UUID.randomUUID()))
                .build();

        List<ThreadEntity> threadEntities = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            int upvotes = 6 - i;

            PostEntity question = PostEntity.builder()
                    .id(UUID.randomUUID())
                    .content("Question " + i)
                    .authorId(UUID.randomUUID())
                    .creationTime(now.minusDays(i))
                    .upvotedByUsers(Collections.nCopies(upvotes, UUID.randomUUID()))
                    .downvotedByUsers(new ArrayList<>())
                    .build();

            QuestionThreadEntity thread = QuestionThreadEntity.builder()
                    .id(UUID.randomUUID())
                    .title("Thread " + i)
                    .creatorId(UUID.randomUUID())
                    .creationTime(now.minusDays(i))
                    .forum(forumEntity)
                    .question(question)
                    .posts(new ArrayList<>())
                    .numberOfPosts(0)
                    .selectedAnswer(null)
                    .build();

            question.setThread(thread);
            threadEntities.add(thread);
        }

        forumEntity.setThreads(threadEntities);

        // Act
        List<de.unistuttgart.iste.meitrex.generated.dto.Thread> result =
                forumService.openQuestions(forumMapper.forumEntityToForum(forumEntity));

        // Assert
        assertThat(result.size(), is(4));

        // Order: Threads with highest Upvotes first
        List<String> expectedTitles = List.of("Thread 0", "Thread 1", "Thread 2", "Thread 3");
        List<String> resultTitles = result.stream()
                .map(de.unistuttgart.iste.meitrex.generated.dto.Thread.class::cast)
                .map(de.unistuttgart.iste.meitrex.generated.dto.Thread::getTitle)
                .toList();

        assertThat(resultTitles, is(expectedTitles));
    }



}
