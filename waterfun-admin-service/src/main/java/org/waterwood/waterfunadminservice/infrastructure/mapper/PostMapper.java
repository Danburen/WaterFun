package org.waterwood.waterfunadminservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunadminservice.api.request.post.CreatePostRequest;
import org.waterwood.waterfunadminservice.api.request.post.PatchUserPostReq;
import org.waterwood.waterfunadminservice.api.response.post.PostResponse;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {


    Post toEntity(CreatePostRequest createPostRequest);

    CreatePostRequest toCreatePostDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(CreatePostRequest createPostRequest, @MappingTarget Post post);

    Post toEntity(PostResponse postResponse);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "tagIds",
            expression = "java(tagsToTagIds(post.getTags()))")
    PostResponse toPostResponseDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PostResponse postResponse, @MappingTarget Post post);

    Post toEntity(PatchUserPostReq patchUserPostReq);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PatchUserPostReq patchUserPostReq, @MappingTarget Post post);

    default Set<Integer> tagsToTagIds(Collection<Tag> tags) {
        return tags.stream().map(Tag::getId).collect(Collectors.toSet());
    }

}