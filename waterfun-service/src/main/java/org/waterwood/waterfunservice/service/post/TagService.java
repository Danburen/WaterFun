package org.waterwood.waterfunservice.service.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.waterwood.waterfunservice.api.request.content.CreateTagRequest;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.List;
import java.util.Set;

public interface TagService {
    /**
     * Create a new tag
     *
     * @param req the tag entity {@link Tag}
     */
    void createTag(CreateTagRequest req);

    /**
     * Return the list ofPending Tags created by current user's tags
     * @return list ofPending {@link Tag}
     */
    List<Tag> getSelfTags();

    /**
     * Get a tag by id
     * @param id tag id
     * @return {@link Tag}
     */
    Tag getTag(Long id);

    /**
     * Update a tag
     * @param tag the tag entity {@link Tag}
     */
    void updateTag(Tag tag);

    /**
     * Delete a tag
     * @param id tag id
     */
    void deleteTag(Long id);

    /**
     * Get tags by tag ids
     * @param tagIds tag ids
     * @return list ofPending {@link Tag}
     */
    Set<Tag> getSelfTags(Iterable<Long> tagIds, boolean strict);

    /**
     * Create new tags by names
     *
     * @param newTags new tags
     * @param userUid creator uid
     * @return saved tags
     */
    List<Tag> createNewTags(Set<String> newTags, Long userUid);

    /**
     * Get tags
     *
     * @param pageable pageable
     * @return list of tagResponse
     */
    Page<Tag> getHotTags(Pageable pageable);

    /**
     * Search for tags
     *
     * @param keyword keywords in name or slug
     * @param limit   limit of per request
     * @return list of optionVOs
     */
    List<Tag> searchTags(String keyword, int limit);
}
