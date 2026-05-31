package org.waterwood.waterfunadminservice.service.content;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.content.CreateCategoryRequest;
import org.waterwood.waterfunadminservice.api.request.content.RemoveCategoriesRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.exception.notfound.NotFoundException;

import java.util.List;

public interface CategoryService {
    /**
     * Create a category.
     * @param req request body
     */
    void create(CreateCategoryRequest req);

    /**
     * List categories.
     * @param spec specification ofPending category
     * @param pageable pageable
     * @return page ofPending categories
     */
    Page<Category> list(Specification<Category> spec, Pageable pageable);


    /**
     * Get a category by id.
     * @param id target category id
     * @return category entity
     * @throws NotFoundException if category not found
     */
    Category getById(Long id);

    /**
     * Remove a category.
     * @param id target id
     */
    void removeById(Long id);

    /**
     * Update a category by id
     * @param id target id
     * @param req request body
     * @throws NotFoundException if category not found
     */
    void update(Long id, UpdateCategoryRequest req);

    /**
     * Batch delete categories by ids
     * @param req request body
     * @return batch operation result
     */
    BatchResult deleteCategories(RemoveCategoriesRequest req);

    /**
     * Get the list ofPending categories OptionVO
     *
     * @return list ofPending categories OptionVO
     */
    List<OptionVO<Long>> getOptions();

}
