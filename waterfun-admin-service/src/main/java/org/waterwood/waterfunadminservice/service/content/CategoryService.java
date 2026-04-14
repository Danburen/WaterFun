package org.waterwood.waterfunadminservice.service.content;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunadminservice.api.request.content.RemoveCategoriesRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.util.List;

public interface CategoryService {
    /**
     * List categories.
     * @param spec specification of category
     * @param pageable pageable
     * @return page of categories
     */
    Page<Category> list(Specification<Category> spec, Pageable pageable);


    /**
     * Get a category by id.
     * @param id target category id
     * @return category entity
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if category not found
     */
    Category getById(Integer id);

    /**
     * Remove a category.
     * @param id target id
     */
    void removeById(Integer id);

    /**
     * Update a category by id
     * @param id target id
     * @param req request body
     * @throws org.waterwood.waterfunservicecore.exception.NotFoundException if category not found
     */
    void update(Integer id, UpdateCategoryRequest req);

    /**
     * Batch delete categories by ids
     * @param req request body
     * @return batch operation result
     */
    BatchResult deleteCategories(RemoveCategoriesRequest req);

    /**
     * Get the list of categories OptionVO
     * @return list of categories OptionVO
     */
    List<OptionVO<Integer>> getOptions();

}
