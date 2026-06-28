package org.waterwood.waterfunservice.service.post.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.waterwood.api.BaseResponseCode;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.waterfunservice.api.response.post.CategoryResponse;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.exception.BizException;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunservicecore.exception.ForbiddenException;
import org.waterwood.waterfunservicecore.exception.notfound.CategoryNotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservice.infrastructure.mapper.CategoryMapper;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservice.service.post.CategoryService;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;
    private final IdentifierGenerator identifierGenerator;

    @Transactional
    @Override
    public void createCategory(Category category) {
        categoryRepository.findByName(category.getName()).ifPresent(_->{
            throw new BizException(BaseResponseCode.POST_CATEGORY_EXISTS);
        });
        category.setCreator(userRepository.getReferenceById(UserCtxHolder.getUserUid()));
        category.setSortOrder(category.getSortOrder());
        category.setName(category.getName());
        category.setSlug(identifierGenerator.generateSlug(category.getName(), categoryRepository));
        category.setDescription(category.getDescription());
        category.setIsActive(category.getIsActive() == null || category.getIsActive());
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> getCategories() {
        List<Category> categories = categoryRepository.findAllByIsDeletedWithParentOrderByUsageCountDesc();
        return categories.stream()
                .map(
                        c -> {
                            CategoryResponse cr = categoryMapper.toResponse(c);
                            if(c.getParent() != null && !c.getParent().getIsDeleted()) {
                                Category parent = c.getParent();
                                cr.setParent(
                                        OptionVO.of(
                                                parent.getId(),
                                                parent.getName(),
                                                parent.getSlug(),
                                                parent.getIsActive()
                                        )
                                );
                            } else {
                                cr.setParent(null);
                            }
                            return cr;
                        }
                ).toList();
    }

    @Override
    public List<OptionVO<Long>> getCategoryOptions() {
        List<Category> categories = categoryRepository.findAllByIsDeletedWithParentOrderByUsageCountDesc();
        return categories.stream()
                .filter(c -> c.getIsActive() && !c.getIsDeleted())
                .map(c -> new OptionVO<>(c.getId(), c.getSlug(), c.getName(), false)
                ).toList();
    }

    @Override
    public Category getCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new);
    }

    @Override
    public void updateCategory(Category category) {
        Category c = categoryRepository.findById(category.getId()).orElseThrow(CategoryNotFoundException::new);
        if (category.getName() != null) c.setName(category.getName());
        if (category.getSlug() != null) c.setSlug(category.getSlug());
        if (category.getDescription() != null) c.setDescription(category.getDescription());
        if (category.getParent() != null) {
            Category parent = categoryRepository.findById(category.getParent().getId()).orElseThrow(
                    () -> new BizException(BaseResponseCode.PARENT_NOT_FOUND, "ID: " + category.getParent().getId())
            );
            c.setParent(parent);
        }
        if (category.getSortOrder() != null) c.setSortOrder(category.getSortOrder());
        if (category.getIsActive() != null) c.setIsActive(category.getIsActive());
        categoryRepository.save(c);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category c = categoryRepository.findById(id).orElseThrow(
                CategoryNotFoundException::new
        );

        if(! c.getCreator().getUid().equals(UserCtxHolder.getUserUid())) {
            throw new ForbiddenException();
        }

        categoryRepository.removeCategoryById(id);
    }
}
