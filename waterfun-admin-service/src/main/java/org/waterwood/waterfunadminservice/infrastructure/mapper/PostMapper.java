package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.utils.StringUtil;
import org.waterwood.waterfunadminservice.api.request.content.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.content.PatchUserPostReq;
import org.waterwood.waterfunadminservice.api.request.content.PutPostReq;
import org.waterwood.waterfunadminservice.api.response.content.PostResponse;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.PostType;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.Collection;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {


    Post toEntity(CreatePostRequest createPostRequest);

    CreatePostRequest toCreatePostDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(CreatePostRequest createPostRequest, @MappingTarget Post post);

    Post toEntity(PostResponse postResponse);

    @Mapping(target = "tagIds",
             expression = "java(tagsToTagIds(post.getTags()))")
    @Mapping(target = "isAnnouncement",
             expression = "java(postToIsAnnouncement(post))")
    @Mapping(target = "authorId",
             expression = "java(post.getAuthor() != null ? post.getAuthor().getUid() : null)")
    @Mapping(target = "categoryId",
             expression = "java(post.getCategory() != null ? post.getCategory().getId() : null)")
    PostResponse toPostResponseDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PostResponse postResponse, @MappingTarget Post post);

    Post toEntity(PatchUserPostReq patchUserPostReq);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PatchUserPostReq patchUserPostReq, @MappingTarget Post post);

    default List<Long> tagsToTagIds(Collection<Tag> tags) {
        return tags.stream().map(Tag::getId).toList();
    }

    default Boolean postToIsAnnouncement(Post post) {
        return post.getType() == PostType.NOTICE;
    }

    Post toEntity(PutPostReq putPostReq);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PutPostReq putPostReq, @MappingTarget Post post);

    @AfterMapping
    default void afterToPostResponseDto(Post post, @MappingTarget PostResponse resp) {
        resp.setSummary(StringUtil.fallbackSummary(resp.getSummary(), post.getContent(), 200));
    }

    @AfterMapping
    default void afterCreatePostRequest(CreatePostRequest req, @MappingTarget Post post) {
        if (Boolean.TRUE.equals(req.getIsAnnouncement())) {
            post.setType(PostType.NOTICE);
        }
    }

    @AfterMapping
    default void afterPartialUpdate(PutPostReq req, @MappingTarget Post post) {
        if (req.getIsAnnouncement() != null) {
            post.setType(req.getIsAnnouncement() ? PostType.NOTICE : PostType.COMMON);
        }
    }

}