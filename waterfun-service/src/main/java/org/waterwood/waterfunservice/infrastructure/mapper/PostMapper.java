package org.waterwood.waterfunservice.infrastructure.mapper;

import org.mapstruct.*;
import org.waterwood.waterfunservice.api.request.PutUserPostReq;
import org.waterwood.waterfunservice.api.response.post.PostAuthorCardResp;
import org.waterwood.waterfunservice.api.response.post.PostCardResp;
import org.waterwood.waterfunservice.api.response.post.PostDetailResp;
import org.waterwood.waterfunservice.api.response.post.PostAuthorDetailResp;
import org.waterwood.waterfunservicecore.entity.post.Post;
import org.waterwood.waterfunservice.api.request.content.PostSaveReq;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.Collection;
import java.util.Set;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PostMapper {


    Post toEntity(PostSaveReq postSaveReq);

    PostSaveReq toCreatePostDto(Post post);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PostSaveReq postSaveReq, @MappingTarget Post post);


    Post toEntity(PutUserPostReq putUserPostReq);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Post partialUpdate(PutUserPostReq putUserPostReq, @MappingTarget Post post);

    default Set<Integer> tagsToTagIds(Collection<Tag> tags) {
        return null;
    }

    PostCardResp toPostCardResponseDto(Post p);

    PostAuthorCardResp toPostAuthorCardResp(Post p);

    PostDetailResp toPostDetailResp(Object post);

    PostAuthorDetailResp toPostAuthorDetailResp(Post post);
}