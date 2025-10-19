package de.unistuttgart.iste.meitrex.media_service.service;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.common.event.ForumActivity;
import de.unistuttgart.iste.meitrex.common.event.ForumActivityEvent;
import de.unistuttgart.iste.meitrex.common.profanity_filter.ProfanityFilter;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.media.MediaRecordEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static de.unistuttgart.iste.meitrex.media_service.controller.ForumController.NOT_FOUND;


@Service
@Slf4j
@RequiredArgsConstructor
public class ForumService {

    private final ModelMapper modelMapper;

    private final ForumRepository forumRepository;
    private final ThreadRepository threadRepository;
    private final PostRepository postRepository;
    private final ThreadContentReferenceRepository threadContentReferenceRepository;
    private final MediaRecordRepository mediaRecordRepository;
    private final TopicPublisher topicPublisher;
    private final ProfanityFilter profanityFilter;

    private final ForumMapper forumMapper;
    private final ThreadMapper threadMapper;
    private final PostMapper postMapper;
    private final QuestionThreadMapper questionThreadMapper;
    private final InfoThreadMapper infoThreadMapper;
    private final QuestionThreadRepository questionThreadRepository;

    /**
     * Add user to Forum
     * @param forumId id of the forum
     * @param userId id of the user
     * @return forum the user was added to
     */
    public Forum addUserToForum(UUID forumId, UUID userId) {
        ForumEntity forum = forumRepository.findById(forumId).orElseThrow(() ->
                new EntityNotFoundException("Forum with id: " + forumId + " not found!"));
        forum.getUserIds().add(userId);
        forum = forumRepository.saveAndFlush(forum);
        return forumMapper.forumEntityToForum(forum);
    }

    /**
     * Add user to Forum
     * @param courseId id of the course of the forum
     * @param userId id of the user
     * @return forum the user was added to
     */
    public Forum addUserToForumCourseId(UUID courseId, UUID userId) {
        ForumEntity forum = forumRepository.findByCourseId(courseId).orElseThrow(() ->
                new EntityNotFoundException("Forum for the course with the id: " + courseId + " not found!"));
        forum.getUserIds().add(userId);
        forum = forumRepository.saveAndFlush(forum);
        return forumMapper.forumEntityToForum(forum);
    }

    public Forum getForumById(UUID id) {
        return forumMapper.forumEntityToForum(forumRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Forum with the id " + id + " not found")));
    }

    /**
     * Returns the forum by the courseId and creates a forum if it does not exists.
     * @param id of the course
     * @return forum of the course
     */
    public Forum getForumByCourseId(UUID id) {
        ForumEntity forum = forumRepository.findByCourseId(id).orElseGet(() -> createForum(id));
        return forumMapper.forumEntityToForum(forum);
    }

    public ThreadEntity getThreadById(UUID id) {
        return threadRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Thread with the id " + id + " not found"));
    }

    /**
     * Returns the thread by the given contentReferences
     * @param threadContentReferenceEntities references between the content and the thread
     * @return threads for the given contentReferences
     */
    public List<Thread> getThreadsByThreadContentReferences(List<ThreadContentReferenceEntity> threadContentReferenceEntities) {
        return threadContentReferenceEntities.stream().map(ThreadContentReferenceEntity::getThread)
                .map(threadMapper::mapThread).toList();
    }

    /**
     * Adds a post to the thread
     * @param post Post that gets added
     * @param thread Thread the post gets added to
     * @param userId Id of the user that writes the post
     * @return Post that was added to the thread
     */
    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        String censored_content = profanityFilter.censor(post.getContent());
        log.info("Censored post content: {}", censored_content);
        PostEntity postEntity = new PostEntity(censored_content, userId, thread);
        postEntity.setReferenceId(post.getReference());
        postEntity = postRepository.save(postEntity);
        log.info("Added Post to thread: {}", postEntity);
        thread.getPosts().add(postEntity);
        thread.setNumberOfPosts(thread.getNumberOfPosts() + 1);
        threadRepository.save(thread);
        ForumActivityEvent event = ForumActivityEvent.builder()
                .forumId(thread.getForum().getId())
                .courseId(thread.getForum().getCourseId())
                .userId(userId)
                .build();
        if (thread instanceof QuestionThreadEntity) {
            event.setActivity(ForumActivity.ANSWER);
        } else if (thread instanceof InfoThreadEntity){
            event.setActivity(ForumActivity.INFO);
        }

        topicPublisher.notifyForumActivity(event);
        return postMapper.mapToPostWithThread(postEntity, thread);
    }

    /**
     * Upvotes a post and deletes the downvote if the user downvoted the post before
     * @param postEntity post that the user upvotes
     * @param userId id of the user
     * @return upvoted post
     */
    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getUpvotedByUsers().contains(userId)) {
            postEntity.getUpvotedByUsers().add(userId);
        } else {
            postEntity.getUpvotedByUsers().remove(userId);
        }
        return postMapper.mapToPost(postRepository.save(postEntity));
    }

    /**
     * Downvotes a post and deletes the upvote if the user downvoted the post before
     * @param postEntity post that the user downvotes
     * @param userId id of the user
     * @return downvoted post
     */
    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getDownvotedByUsers().contains(userId)) {
            postEntity.getDownvotedByUsers().add(userId);
        } else {
            postEntity.getDownvotedByUsers().remove(userId);
        }
        return postMapper.mapToPost(postRepository.save(postEntity));
    }

    /**
     * Creates a question Thread
     * @param thread thread that gets created
     * @param forum forum the thread gets created in
     * @param userId user that creates the thread
     * @return created thread
     */
    public QuestionThread createQuestionThread(InputQuestionThread thread, ForumEntity forum ,UUID userId) {
        String question_censored = profanityFilter.censor(thread.getQuestion().getContent());
        String title_censored = profanityFilter.censor(thread.getTitle());
        PostEntity questionEntity = new PostEntity(question_censored, userId);
        QuestionThreadEntity threadEntity = new QuestionThreadEntity(forum, userId, title_censored, questionEntity);
        questionEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }
        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        topicPublisher.notifyForumActivity(ForumActivityEvent.builder()
                        .userId(userId)
                        .forumId(forum.getId())
                        .courseId(forum.getCourseId())
                        .activity(ForumActivity.QUESTION)
                .build());
        return questionThreadMapper.mapQuestionThread(threadEntity);
    }

    /**
     * Creates a info Thread
     * @param thread thread that gets created
     * @param forum forum the thread gets created in
     * @param userId user that creates the thread
     * @return created thread
     */
    public InfoThread createInfoThread(InputInfoThread thread, ForumEntity forum, UUID userId) {
        String info_censored = profanityFilter.censor(thread.getInfo().getContent());
        String title_censored = profanityFilter.censor(thread.getTitle());
        PostEntity infoEntity = new PostEntity(info_censored, userId);
        InfoThreadEntity threadEntity = new InfoThreadEntity(forum, userId, title_censored, infoEntity);
        infoEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }

        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        topicPublisher.notifyForumActivity(ForumActivityEvent.builder()
                .userId(userId)
                .forumId(forum.getId())
                .courseId(forum.getCourseId())
                .activity(ForumActivity.THREAD)
                .build());

        return infoThreadMapper.mapInfoThread(threadEntity);
    }

    private void addThreatToContentOnThreadCreation(ThreadEntity thread, UUID contentId, Integer timeStampSeconds, Integer pageNumber) {
        List<MediaRecordEntity> mediaRecordEntities = mediaRecordRepository
                .findMediaRecordEntitiesByContentIds(List.of(contentId));
        mediaRecordEntities.stream().findAny().orElseThrow(()-> new EntityNotFoundException("MediaRecord that includes content with the id "
                + contentId + " not found"));
        mediaRecordEntities.stream().map(MediaRecordEntity::getCourseIds).flatMap(List::stream)
                .filter(courseId -> courseId.equals(thread.getForum().getCourseId())).findAny().orElseThrow(() ->
                        new EntityNotFoundException("Content with the id " + contentId
                                + " not in course " + thread.getForum().getCourseId()));
        thread.setThreadContentReference(new ThreadContentReferenceEntity(thread, contentId, timeStampSeconds, pageNumber));
    }

    /**
     * Updates the given post
     * @param post updates to the post
     * @param postEntity the post that gets updated
     * @param userId user that updates the post
     * @return updated post
     * @throws AuthenticationException if another user tries to change the post
     */
    public Post updatePost(InputPost post, PostEntity postEntity, UUID userId) throws AuthenticationException {
        if (!postEntity.getAuthorId().equals(userId)) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        String content_censored = profanityFilter.censor(post.getContent());
        postEntity.setContent(content_censored);
        postEntity.setEdited(true);
        return postMapper.mapToPost(postRepository.save(postEntity));
    }

    /**
     * Deletes the given post
     * @param post post that gets deleted
     * @param user user that deletes the ost
     * @return deleted post
     * @throws AuthenticationException if the user is different of the user that created the post and not superuser or course creator
     */
    public Post deletePost(PostEntity post, LoggedInUser user) throws AuthenticationException {
        if (!post.getAuthorId().equals(user.getId())
                && !(user.getRealmRoles().contains(LoggedInUser.RealmRole.COURSE_CREATOR)
                || user.getRealmRoles().contains(LoggedInUser.RealmRole.SUPER_USER))) {
            throw new AuthenticationException("User is not authorized to delete this post");
        }
        Post realPost = postMapper.mapToPost(post);
        ThreadEntity thread = post.getThread();
        thread.getPosts().remove(post);
        thread.setNumberOfPosts(thread.getNumberOfPosts() - 1);
        threadRepository.save(thread);
        post.setThread(null);
        postRepository.delete(post);
        return realPost;
    }

    /**
     * Deletes the given thread
     * @param thread thread that gets deleted
     * @param user user that deletes the thread
     * @return deleted thread
     * @throws AuthenticationException if the user is different of the user that created the thread and not superuser or course creator
     */
    public Thread deleteThread(ThreadEntity thread, LoggedInUser user) throws AuthenticationException {
        if (!thread.getCreatorId().equals(user.getId())
                && !(user.getRealmRoles().contains(LoggedInUser.RealmRole.COURSE_CREATOR)
                || user.getRealmRoles().contains(LoggedInUser.RealmRole.SUPER_USER))) {
            throw new AuthenticationException("User is not authorized to delete this thread");
        }
        Thread realThread = threadMapper.mapThread(thread);
        ForumEntity forum = thread.getForum();
        forum.getThreads().remove(thread);
        threadRepository.delete(thread);
        forumRepository.save(forum);
        return realThread;
    }

    /**
     * Adds thread to a contentId
     * @param thread thread that gets added to the content
     * @param contentId id of the content
     * @param timeStamp timestamp of the video where the thread is created
     * @param pageNumber pageNumber of the slides where the thread is created
     * @return reference between the thread and the content
     */
    public ThreadContentReference addThreadToContent(ThreadEntity thread, UUID contentId, Integer timeStamp, Integer pageNumber) {
        ThreadContentReferenceEntity threadContentReferenceEntity = new ThreadContentReferenceEntity(thread, contentId, timeStamp, pageNumber);
        threadContentReferenceRepository.save(threadContentReferenceEntity);
        thread.setThreadContentReference(threadContentReferenceEntity);
        threadRepository.save(thread);
        return modelMapper.map(threadContentReferenceEntity, ThreadContentReference.class);
    }

    /**
     * Selects the answer for a question thread
     * @param postId post that is the answer
     * @return QuestionThread where the post was selected
     */
    public QuestionThread addAnswerToQuestionThread(UUID postId) {
        PostEntity answer = postRepository.findById(postId).orElseThrow(
                () -> new EntityNotFoundException("Post with the id " + postId + NOT_FOUND)
        );
        log.info(answer.getThread().toString());
        QuestionThreadEntity questionThread = questionThreadRepository.findById(answer.getThread().getId()).orElseThrow(
                () -> new EntityNotFoundException("QuestionThread with the id " + answer.getThread().getId() + NOT_FOUND));
        if (questionThread.getSelectedAnswer() != null && questionThread.getSelectedAnswer().getId().equals(answer.getId())) {
            questionThread.setSelectedAnswer(null);
        } else {
            questionThread.setSelectedAnswer(answer);
            ForumActivityEvent forumActivityEvent = ForumActivityEvent.builder()
                    .userId(answer.getAuthorId())
                    .forumId(questionThread.getForum().getId())
                    .courseId(questionThread.getForum().getCourseId())
                    .activity(ForumActivity.ANSWER_ACCEPTED)
                    .build();
            topicPublisher.notifyForumActivity(forumActivityEvent);
        }
        questionThread = threadRepository.save(questionThread);

        return questionThreadMapper.mapQuestionThread(questionThread);
    }

    private ForumEntity createForum(UUID courseId) {
        ForumEntity forum = new ForumEntity(courseId);
        forum = forumRepository.save(forum);
        return forum;
    }

    /**
     * Last 4 activities of the forum
     * @param forum forum with the activities
     * @return up to 4 activities
     */
    public List<ForumActivityEntry> forumActivity(Forum forum) {
        List<ForumActivityEntry> activities = new ArrayList<>();

        List<Thread> threads = forum.getThreads();
        if (threads == null) {
            threads = Collections.emptyList();
        }

        for (Thread thread : threads) {
            activities.add(new ForumActivityEntry(thread.getCreationTime(), thread, null, null));

            List<Post> posts = thread.getPosts();
            for (Post post : posts) {
                activities.add(new ForumActivityEntry(post.getCreationTime(), thread, post, null));
            }
        }

        activities.sort(Comparator.comparing(ForumActivityEntry::getCreationTime).reversed());
        return activities.stream().limit(4).toList();
    }


    /**
     * Returns the forum activities of the other user
     * @param userId user that requests the activities
     * @param otherUserId user the activities have been requested
     * @return list of forum activities
     */
    public List<ForumActivityEntry> otherUserForumActivityByUserId(UUID userId, UUID otherUserId) {
        List<ForumEntity> forumEntitiesUser = forumRepository.findAllByUserIdsContaining(userId);
        List<ForumEntity> forumEntitiesOtherUser = forumRepository.findAllByUserIdsContaining(otherUserId);

        Set<UUID> userForumIds = forumEntitiesUser.stream()
                .map(ForumEntity::getId)
                .collect(Collectors.toSet());

        List<ForumEntity> commonForums = forumEntitiesOtherUser.stream()
                .filter(f -> userForumIds.contains(f.getId()))
                .collect(Collectors.toList());

        List<ForumActivityEntry> activities = extractForumActivitiesForUser(
                commonForums,
                otherUserId,
                forumMapper
        );

        activities.sort(Comparator.comparing(ForumActivityEntry::getCreationTime).reversed());

        return activities;
    }

    /**
     * Returns the forum activities of a user
     * @param userId user that did the forum activities
     * @return list of forum activities
     */
    public List<ForumActivityEntry> forumActivityByUserId(UUID userId) {
        List<ForumEntity> forumEntities = forumRepository.findAllByUserIdsContaining(userId);

        List<ForumActivityEntry> activities = extractForumActivitiesForUser(
                forumEntities,
                userId,
                forumMapper
        );

        activities.sort(Comparator.comparing(ForumActivityEntry::getCreationTime).reversed());
        return activities;
    }

    private List<ForumActivityEntry> extractForumActivitiesForUser(
            List<ForumEntity> forumEntities,
            UUID userId,
            ForumMapper forumMapper
    ) {
        List<ForumActivityEntry> activities = new ArrayList<>();

        for (ForumEntity forumEntity : forumEntities) {
            Forum forum = forumMapper.forumEntityToForum(forumEntity);
            UUID courseId = forum.getCourseId();

            for (Thread thread : forum.getThreads()) {
                if (thread.getCreatorId().equals(userId)) {
                    activities.add(new ForumActivityEntry(
                            thread.getCreationTime(),
                            thread,
                            null,
                            courseId
                    ));
                }

                for (Post post : thread.getPosts()) {
                    if (post.getAuthorId().equals(userId)) {
                        activities.add(new ForumActivityEntry(
                                post.getCreationTime(),
                                thread,
                                post,
                                courseId
                        ));
                    }
                }
            }
        }

        return activities;
    }

    /**
     * Returns the open questions for a forum
     * @param forum forum the questions are open in
     * @return list of threads with open questions
     */
    public List<Thread> openQuestions(Forum forum) {
        double alpha = 0.6;
        double beta = 0.4;

        List<QuestionThread> questionThreads = forum.getThreads().stream()
                .filter(t -> t instanceof QuestionThread)
                .map(t -> (QuestionThread) t)
                .filter(qt -> qt.getSelectedAnswer() == null)
                .toList();

        int maxUpvotes = questionThreads.stream()
                .mapToInt(qt -> qt.getQuestion().getUpvotedByUsers().size() - qt.getQuestion().getDownvotedByUsers().size())
                .max()
                .orElse(1);

        return questionThreads.stream()
                .sorted((qt1, qt2) -> {
                    double score1 = calculatePriorityScore(qt1, maxUpvotes, alpha, beta);
                    double score2 = calculatePriorityScore(qt2, maxUpvotes, alpha, beta);
                    return Double.compare(score2, score1);
                })
                .limit(4)
                .collect(Collectors.toList());
    }

    /*
     Ranks question threads based on their age and popularity (upvotes)
     Formular: priorityScore = α × ageScore + β × upvoteScore
     Age Score (Gaussian distribution): favors questions around 7 days old, penalizes very new or very old ones
     Upvote Score:
        Positive votes scaled linearly between 0.1 and 1.0
        Negative votes penalized quadratically but never drop below 0.01
     */
    private double calculatePriorityScore(QuestionThread qt, int maxUpvotes, double alpha, double beta) {
        long ageInDays = Duration.between(qt.getCreationTime().toInstant(), Instant.now()).toDays();

        double peakAge = 7.0;
        double spread = 15.0;
        double ageScore = Math.exp(-Math.pow((ageInDays - peakAge) / spread, 2));  // ∈ (0, 1]

        int upvotes = qt.getQuestion().getUpvotedByUsers().size() - qt.getQuestion().getDownvotedByUsers().size();
        double upvoteScore;

        // ∈ (0, 1]
        if (upvotes >= 0) {
            upvoteScore = 0.1 + 0.9 * ((double) upvotes / maxUpvotes); // ∈ (0.1, 1]
        } else {
            upvoteScore = Math.max(0.01, 1.0 / (1.0 + 10 * Math.pow(-upvotes, 2))); // ∈ (0.01, 0.1]
        }

        return alpha * ageScore + beta * upvoteScore;
    }
}

