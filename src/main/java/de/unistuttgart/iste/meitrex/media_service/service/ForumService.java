package de.unistuttgart.iste.meitrex.media_service.service;
import java.util.Comparator;
import java.util.stream.Collectors;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.generated.dto.Thread;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ForumMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.mapper.ThreadMapper;
import de.unistuttgart.iste.meitrex.media_service.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.naming.AuthenticationException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;


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

    private final ForumMapper forumMapper;
    private final ThreadMapper threadMapper;

    public Forum addUserToForum(UUID forumId, UUID userId) {
        ForumEntity forum = forumRepository.findById(forumId).orElseThrow(() ->
                new EntityNotFoundException("Forum with id: " + forumId + " not found!"));
        forum.getUserIds().add(userId);
        forum = forumRepository.saveAndFlush(forum);
        return forumMapper.forumEntityToForum(forum);
    }

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

    public Forum getForumByCourseId(UUID id) {
        ForumEntity forum = forumRepository.findByCourseId(id).orElseGet(() -> createForum(id));
        return forumMapper.forumEntityToForum(forum);
    }

    public ThreadEntity getThreadById(UUID id) {
        return threadRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Thread with the id " + id + " not found"));
    }

    public List<Thread> getThreadsByThreadContentReferences(List<ThreadContentReferenceEntity> threadContentReferenceEntities) {
        return threadContentReferenceEntities.stream().map(ThreadContentReferenceEntity::getThread)
                .map(threadMapper::mapThread).toList();
    }

    public Post addPostToThread(InputPost post, ThreadEntity thread, UUID userId) {
        PostEntity postEntity = new PostEntity(post.getContent(), userId, thread);
        postEntity = postRepository.save(postEntity);
        thread.getPosts().add(postEntity);
        thread.setNumberOfPosts(thread.getNumberOfPosts() + 1);
        threadRepository.save(thread);
        return modelMapper.map(postEntity, Post.class);
    }

    public Post upvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getDownvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getUpvotedByUsers().contains(userId)) {
            postEntity.getUpvotedByUsers().add(userId);
        } else {
            postEntity.getUpvotedByUsers().remove(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post downvotePost(PostEntity postEntity, UUID userId) {
        postEntity.getUpvotedByUsers().removeIf(id -> id.equals(userId));
        if (!postEntity.getDownvotedByUsers().contains(userId)) {
            postEntity.getDownvotedByUsers().add(userId);
        } else {
            postEntity.getDownvotedByUsers().remove(userId);
        }
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public QuestionThread createQuestionThread(InputQuestionThread thread, ForumEntity forum, UUID userId) {
        PostEntity questionEntity = new PostEntity(thread.getQuestion().getContent(), userId);
        QuestionThreadEntity threadEntity = new QuestionThreadEntity(forum, userId, thread.getTitle(), questionEntity);
        questionEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }
        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        return modelMapper.map(threadEntity, QuestionThread.class);
    }

    public InfoThread createInfoThread(InputInfoThread thread, ForumEntity forum, UUID userId) {
        PostEntity infoEntity = new PostEntity(thread.getInfo().getContent(), userId);
        InfoThreadEntity threadEntity = new InfoThreadEntity(forum, userId, thread.getTitle(), infoEntity);
        infoEntity.setThread(threadEntity);

        if (thread.getThreadContentReference() != null){
            addThreatToContentOnThreadCreation(threadEntity, thread.getThreadContentReference().getContentId(),
                    thread.getThreadContentReference().getTimeStampSeconds(),
                    thread.getThreadContentReference().getPageNumber());
        }

        threadEntity = threadRepository.save(threadEntity);
        forum.getThreads().add(threadEntity);
        forumRepository.save(forum);

        return modelMapper.map(threadEntity, InfoThread.class);
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

    public Post updatePost(InputPost post, PostEntity postEntity, UUID userId) throws AuthenticationException {
        if (!postEntity.getAuthorId().equals(userId)) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        postEntity.setContent(post.getContent());
        postEntity.setEdited(true);
        return modelMapper.map(postRepository.save(postEntity), Post.class);
    }

    public Post deletePost(PostEntity post, LoggedInUser user) throws AuthenticationException {
        if (!post.getAuthorId().equals(user.getId())
                && !(user.getRealmRoles().contains(LoggedInUser.RealmRole.COURSE_CREATOR)
                || user.getRealmRoles().contains(LoggedInUser.RealmRole.SUPER_USER))) {
            throw new AuthenticationException("User is not authorized to update this post");
        }
        Post realPost = modelMapper.map(post, Post.class);
        ThreadEntity thread = post.getThread();
        thread.getPosts().remove(post);
        thread.setNumberOfPosts(thread.getNumberOfPosts() - 1);
        threadRepository.save(thread);
        postRepository.delete(post);
        return realPost;
    }

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

    public ThreadContentReference addThreadToContent(ThreadEntity thread, UUID contentId, Integer timeStamp, Integer pageNumber) {
        ThreadContentReferenceEntity threadContentReferenceEntity = new ThreadContentReferenceEntity(thread, contentId, timeStamp, pageNumber);
        threadContentReferenceRepository.save(threadContentReferenceEntity);
        thread.setThreadContentReference(threadContentReferenceEntity);
        threadRepository.save(thread);
        return modelMapper.map(threadContentReferenceEntity, ThreadContentReference.class);
    }

    public QuestionThread addAnserToQuestionThread(QuestionThreadEntity questionThread, PostEntity answer) {
        questionThread.setSelectedAnswer(answer);
        questionThread = threadRepository.save(questionThread);
        return modelMapper.map(questionThread, QuestionThread.class);
    }

    private ForumEntity createForum(UUID courseId) {
        ForumEntity forum = new ForumEntity(courseId);
        forum = forumRepository.save(forum);
        return forum;
    }

    public List<ForumActivityEntry> forumActivity(Forum forum) {
        List<ForumActivityEntry> activities = new ArrayList<>();

        List<Thread> threads = forum.getThreads();
        if (threads == null) {
            threads = Collections.emptyList();
        }

        for (Thread thread : threads) {
            activities.add(new ForumActivityEntry(thread.getCreationTime(), thread, null, null));

            List<Post> posts = thread.getPosts();
            if (posts == null) {
                posts = Collections.emptyList();
            }

            for (Post post : posts) {
                activities.add(new ForumActivityEntry(post.getCreationTime(), thread, post, null));
            }
        }

        activities.sort(Comparator.comparing(ForumActivityEntry::getCreationTime).reversed());
        return activities.stream().limit(4).toList();
    }

    /*
    public List<ForumActivityEntry> otherUserForumActivityByUserId(UUID userId, UUID otherUserId) {

    }
       */
    /*
     This is not very performant because we loop through each forum, thread and posts and search for matches
     We should add a user list to each forum and go from there on!
     */
    public List<ForumActivityEntry> forumActivityByUserId(UUID userId) {
        List<ForumActivityEntry> activities = new ArrayList<>();

        List<ForumEntity> forumEntities = forumRepository.findAllByUserIdsContaining(userId);

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
        activities.sort(Comparator.comparing(ForumActivityEntry::getCreationTime).reversed());
        return activities;
    }

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

        List<Thread> openQuestions = questionThreads.stream()
                .sorted((qt1, qt2) -> {
                    double score1 = calculatePriorityScore(qt1, maxUpvotes, alpha, beta);
                    double score2 = calculatePriorityScore(qt2, maxUpvotes, alpha, beta);
                    return Double.compare(score2, score1);
                })
                .limit(4)
                .collect(Collectors.toList());

        return openQuestions;
    }

    /*
     Ranks question threads based on their age and popularity (upvotes)
     Formular: priorityScore = α × ageScore + β × upvoteScore
     Age Score (Gaussian distribution): favors questions around 7 days old, penalizes very new or very old ones
        E.g.: 7 days -> ageScore = 1
        E.g.: 0 days  -> ageScore = 0.804
        E.g.: 22 days -> ageScore = 0.367
     Upvote Score:
        Positive votes scaled linearly between 0.1 and 1.0
        Negative votes penalized quadratically but never drop below 0.01
            E.g.: upvotes = +10 maxUpvotes = 20 -> upvoteScore = 0.55
            E.g.: upvotes = +1 maxUpvotes = 10 -> upvoteScore = 0.19
            E.g.: upvotes = 0 maxUpvotes = 10 -> upvoteScore = 0.1
            E.g.: upvotes = -1 maxUpvotes = 10 -> upvoteScore = 0.0909
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

