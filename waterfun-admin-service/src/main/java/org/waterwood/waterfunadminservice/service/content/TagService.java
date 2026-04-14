package org.waterwood.waterfunadminservice.service.content;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.content.DeleteTagsRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateTagReq;
import org.waterwood.waterfunservicecore.entity.post.Tag;

import java.util.List;

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

    /**
     * Batch remove tags
     * @param req request body containing tag ids to delete
     * @return batch operation result
     */
    BatchResult deleteTags(DeleteTagsRequest req);

    /**
     * Get the tag options
     * @return list of optionVOs
     */
    List<OptionVO<Integer>> getOptions();
}
