package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import javax.naming.AuthenticationException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForumServiceTest {
    private final ForumRepository forumRepository = mock(ForumRepository.class);
    private final ThreadRepository threadRepository = mock(ThreadRepository.class);
    private final PostRepository postRepository = mock(PostRepository.class);
    private final MediaRecordRepository mediaRecordRepository = mock(MediaRecordRepository.class);
    private final ThreadMediaRecordReferenceRepository threadMediaRecordReferenceRepository = mock(ThreadMediaRecordReferenceRepository.class);

    private final ModelMapper modelMapper = new ModelMapper();
    private final ThreadMapper threadMapper = new ThreadMapper(modelMapper);
    private final ForumMapper forumMapper = new ForumMapper(threadMapper);


    private final ForumService forumService = new ForumService(modelMapper, forumRepository, threadRepository,
            postRepository, mediaRecordRepository, threadMediaRecordReferenceRepository, forumMapper, threadMapper);

    @Test
    void testGetForumById() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
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
                .build();
        final ThreadEntity thread = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(List.of())
                .threadMediaRecordReference(null)
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
        MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .name("Example Record1")
                .courseIds(new ArrayList<>(List.of(UUID.randomUUID())))
                .creatorId(UUID.randomUUID())
                .type(MediaRecordEntity.MediaType.DOCUMENT)
                .contentIds(new ArrayList<>(List.of(UUID.randomUUID())))
                .build();
        final ThreadMediaRecordReferenceEntity threadMediaRecordReference = new ThreadMediaRecordReferenceEntity(thread, mediaRecord, null, null);

        thread.setThreadMediaRecordReference(threadMediaRecordReference);
        when(threadMediaRecordReferenceRepository.findAllByMediaRecord(mediaRecord)).thenReturn(List.of(threadMediaRecordReference));
        assertThat(forumService.getThreadsByMediaRecord(mediaRecord).getFirst(), is(threadMapper.mapThread(thread)));
    }

    @Test
    void testAddPostToThread() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
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

        assertThat(modelMapper.map(forumService.addPostToThread(post,threadEntity,postEntity.getAuthorId()), PostEntity.class), is(finalPostEntity));
    }

    @Test
    void testUpvotePost() {
        final ForumEntity forum = ForumEntity.builder()
                .id(UUID.randomUUID())
                .courseId(UUID.randomUUID())
                .threads(List.of())
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
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
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
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
                .build();

        final QuestionThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("TestTitle")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .forum(forum)
                .threadMediaRecordReference(null)
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
                .build();

        final InfoThreadEntity threadEntity = InfoThreadEntity.builder()
                .id(UUID.randomUUID())
                .title("TestTitle")
                .creatorId(UUID.randomUUID())
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .forum(forum)
                .threadMediaRecordReference(null)
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
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
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
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
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
                .build();

        final ThreadEntity threadEntity = QuestionThreadEntity.builder()
                .id(UUID.randomUUID())
                .forum(forum)
                .creatorId(UUID.randomUUID())
                .title("TestTitle")
                .creationTime(OffsetDateTime.now())
                .posts(new ArrayList<>())
                .threadMediaRecordReference(null)
                .numberOfPosts(0)
                .build();

        final MediaRecordEntity mediaRecord = MediaRecordEntity.builder()
                .id(UUID.randomUUID())
                .build();

        final int timeStamp = 10;
        final int pageNumber = 3;

        final ThreadMediaRecordReferenceEntity threadMediaRecordReference = new ThreadMediaRecordReferenceEntity(threadEntity, mediaRecord, timeStamp, pageNumber);

        assertThat(forumService.addThreadToMediaRecord(threadEntity, mediaRecord, timeStamp, pageNumber), is(modelMapper.map(threadMediaRecordReference, ThreadMediaRecordReference.class)));
    }
}
