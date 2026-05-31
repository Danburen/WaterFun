package org.waterwood.waterfunservice.service.post;

import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.List;
import java.util.Set;

public interface TagService {
    /**
     * Create a new tag
     * @param tag the tag entity {@link Tag}
     */
    void createTag(Tag tag);

    /**
     * Return the list ofPending Tags created by current user's tags
     * @return list ofPending {@link Tag}
     */
    List<Tag> getTags();

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
    Set<Tag> getTags(Iterable<Long> tagIds, boolean strict);

    /**
     * Create new tags by names
     *
     * @param newTags new tags
     * @param userUid creator uid
     * @return saved tags
     */
    List<Tag> createNewTags(Set<String> newTags, Long userUid);
}
