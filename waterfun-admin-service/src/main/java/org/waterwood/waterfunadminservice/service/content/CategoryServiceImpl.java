package org.waterwood.waterfunadminservice.service.content;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.entity.post.Category;
import org.waterwood.waterfunservicecore.exception.NotFoundException;
import org.waterwood.waterfunservicecore.infrastructure.persistence.CategoryRepository;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

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
}
