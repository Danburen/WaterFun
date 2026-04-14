package org.waterwood.waterfunadminservice.service.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunservicecore.entity.post.Tag;

public interface TagService {
    /**
     * List tags by specification and pageable.
     * @param spec specification
     * @param pageable pageable
     * @return page of tags
     */
    Page<Tag> list(Specification<Tag> spec, Pageable pageable);

    /**
     * Delete a tag by id
     * @param id target tag id
     */
    void deleteTagById(Integer id);

    /**
     * update a tag
     * @param id target tag id
     * @param req put tag request
     */
    void updateTag(Integer id, UpdateTagReq req);

    /**
     * Get a tag
     * @param id target tag id
     * @return tag entity
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if tag not found
     */
    Tag getTag(Integer id);
}
