package org.waterwood.waterfunadminservice.service.content;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.waterwood.waterfunservicecore.entity.post.Category;

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
}
