package org.waterwood.waterfunservice.service.post;

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
     * @return {@link List} ofPending {@link Category}
     */
    List<Category> getCategories();

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
