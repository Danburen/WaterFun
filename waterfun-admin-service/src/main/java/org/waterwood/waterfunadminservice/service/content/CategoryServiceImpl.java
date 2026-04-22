package org.waterwood.waterfunadminservice.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.api.VO.BatchResult;
import org.waterwood.api.VO.OptionVO;
import org.waterwood.utils.CollectionUtil;
import org.waterwood.utils.generator.IdentifierGenerator;
import org.waterwood.waterfunadminservice.api.request.content.CreateCategoryRequest;
import org.waterwood.waterfunadminservice.api.request.content.RemoveCategoriesRequest;
import org.waterwood.waterfunadminservice.api.request.content.UpdateCategoryRequest;
import org.waterwood.waterfunadminservice.infrastructure.mapper.CategoryMapper;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;
import org.waterwood.waterfunservicecore.infrastructure.utils.context.UserCtxHolder;
import org.waterwood.waterfunservicecore.services.user.UserCoreService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final IdentifierGenerator identifierGenerator;
    private final UserCoreService userCoreService;

    @Override
    public void create(CreateCategoryRequest req) {
        Category category = categoryMapper.toEntity(req);
        category.setSlug(identifierGenerator.fromSlug(req.getSlug(), req.getName(), categoryRepository));
        category.setCreator(userCoreService.getUserByUid(UserCtxHolder.getUserUid()));
        if (req.getParentId() != null) {
            category.setParent(getById(req.getParentId()));
        }
        categoryRepository.save(category);
    }

    @Override
    public Page<Category> list(Specification<Category> spec, Pageable pageable) {
        return categoryRepository.findAll(spec, pageable);
    }

    @Override
    public Category getById(Integer id) {
        return categoryRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Category ID: " + id)
        );
    }

    @Override
    public void removeById(Integer id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public void update(Integer id, UpdateCategoryRequest req) {
        Category category = getById(id);
        categoryMapper.partialUpdate(req, category);
        if(req.getParentId() != null) {
            Category parent = getById(req.getParentId());
            category.setParent(parent);
        }
        categoryRepository.save(category);
    }

    @Override
    public BatchResult deleteCategories(RemoveCategoriesRequest req) {
        int removed = 0;
        if(CollectionUtil.isNotEmpty(req.getCategoryIds())){
            removed = categoryRepository.deleteByIdIn(req.getCategoryIds());
        }
        return BatchResult.ofNullable(req.getCategoryIds(), removed);
    }

    @Override
    public List<OptionVO<Integer>> getOptions() {
        return categoryRepository.findAll().stream()
                .filter(c -> ! c.getIsDeleted())
                .map(c -> {
                  return OptionVO.<Integer>builder()
                          .id(c.getId())
                          .name(c.getName())
                          .code(c.getSlug())
                          .build();
                })
                .toList();
    }
}
