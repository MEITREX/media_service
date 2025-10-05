package de.unistuttgart.iste.meitrex.media_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.Post;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.PostEntity;
import de.unistuttgart.iste.meitrex.media_service.persistence.entity.forum.ThreadEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostMapper {
    public Post mapToPost(PostEntity postEntity) {
        Post post = Post.builder()
                .setId(postEntity.getId())
                .setContent(postEntity.getContent())
                .setAuthorId(postEntity.getAuthorId())
                .setEdited(postEntity.isEdited())
                .setCreationTime(postEntity.getCreationTime())
                .setDownvotedByUsers(postEntity.getDownvotedByUsers())
                .setUpvotedByUsers(postEntity.getUpvotedByUsers())
                .setReference(null)
                .build();
        if (postEntity.getReferenceId() != null) {
            PostEntity referenceEntity = postEntity.getThread().getPosts().stream().filter(postEntity1 -> postEntity1
                    .getId().equals(postEntity.getReferenceId())).findFirst().orElse(null);
            if (referenceEntity != null) {
                Post postReference = Post.builder()
                        .setId(referenceEntity.getId())
                        .setContent(referenceEntity.getContent())
                        .setAuthorId(referenceEntity.getAuthorId())
                        .setEdited(referenceEntity.isEdited())
                        .setCreationTime(referenceEntity.getCreationTime())
                        .setDownvotedByUsers(referenceEntity.getDownvotedByUsers())
                        .setUpvotedByUsers(referenceEntity.getUpvotedByUsers())
                        .setReference(null)
                        .build();
                post.setReference(postReference);
            }
        }
        return post;
    }

    public Post mapToPostWithThread(PostEntity postEntity, ThreadEntity threadEntity) {
        Post post = Post.builder()
                .setId(postEntity.getId())
                .setContent(postEntity.getContent())
                .setAuthorId(postEntity.getAuthorId())
                .setEdited(postEntity.isEdited())
                .setCreationTime(postEntity.getCreationTime())
                .setDownvotedByUsers(postEntity.getDownvotedByUsers())
                .setUpvotedByUsers(postEntity.getUpvotedByUsers())
                .setReference(null)
                .build();
        if (postEntity.getReferenceId() != null) {
            PostEntity referenceEntity = threadEntity.getPosts().stream().filter(postEntity1 -> postEntity1
                    .getId().equals(postEntity.getReferenceId())).findFirst().orElse(null);
            if (referenceEntity != null) {
                Post postReference = Post.builder()
                        .setId(referenceEntity.getId())
                        .setContent(referenceEntity.getContent())
                        .setAuthorId(referenceEntity.getAuthorId())
                        .setEdited(referenceEntity.isEdited())
                        .setCreationTime(referenceEntity.getCreationTime())
                        .setDownvotedByUsers(referenceEntity.getDownvotedByUsers())
                        .setUpvotedByUsers(referenceEntity.getUpvotedByUsers())
                        .setReference(null)
                        .build();
                post.setReference(postReference);
            }
        }
        return post;
    }

    public List<Post> mapToPosts(List<PostEntity> postEntities) {
        List<Post> posts = new ArrayList<>();
        for  (PostEntity postEntity : postEntities) {
            Post post = Post.builder()
                    .setId(postEntity.getId())
                    .setContent(postEntity.getContent())
                    .setAuthorId(postEntity.getAuthorId())
                    .setEdited(postEntity.isEdited())
                    .setCreationTime(postEntity.getCreationTime())
                    .setDownvotedByUsers(postEntity.getDownvotedByUsers())
                    .setUpvotedByUsers(postEntity.getUpvotedByUsers())
                    .setReference(null)
                    .build();
            if (postEntity.getReferenceId() != null) {
                PostEntity referenceEntity = postEntities.stream().filter(postEntity1 -> postEntity1
                        .getId().equals(postEntity.getReferenceId())).findFirst().orElse(null);
                if (referenceEntity != null) {
                    Post postReference = Post.builder()
                            .setId(referenceEntity.getId())
                            .setContent(referenceEntity.getContent())
                            .setAuthorId(referenceEntity.getAuthorId())
                            .setEdited(referenceEntity.isEdited())
                            .setCreationTime(referenceEntity.getCreationTime())
                            .setDownvotedByUsers(referenceEntity.getDownvotedByUsers())
                            .setUpvotedByUsers(referenceEntity.getUpvotedByUsers())
                            .setReference(null)
                            .build();
                    post.setReference(postReference);
                }
            }
            posts.add(post);
        }
        return posts;
    }
}
