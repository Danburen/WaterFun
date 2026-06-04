package org.waterwood.waterfunservice.service.post;

import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservice.api.response.post.CategoryResponse;
import org.waterwood.waterfunservicecore.entity.post.Category;

import java.util.List;

/**
 * Category Service
 */
public interface CategoryService {
    /**
     * Create a new category
     * the category belong to current user;
     * @param category the category entity {@link Category}
     */
    void createCategory(Category category);

    /**
     * Get all categories
     *
     * @return {@link List} ofPending {@link Category}
     */
    List<CategoryResponse> getCategories();

    /**
     * List all {@link OptionVO} of categories
     * @return list of optionVOs
     */
    List<OptionVO<Long>> getCategoryOptions();

    /**
     * Get a category by id
     * @param id the category id
     * @return {@link Category}
     */
    Category getCategory(Long id);

    /**
     * Update a category
     * @param category the category entity {@link Category}
     */
    void updateCategory(Category category);

    /**
     * Delete a category
     * @param id the category id
     */
    void deleteCategory(Long id);
}
