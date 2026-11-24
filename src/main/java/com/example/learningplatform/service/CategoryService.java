package com.example.learningplatform.service;

import com.example.learningplatform.dto.NameRequest;
import com.example.learningplatform.dto.NamedResourceResponse;
import com.example.learningplatform.entity.Category;
import com.example.learningplatform.exception.DuplicateResourceException;
import com.example.learningplatform.exception.ResourceNotFoundException;
import com.example.learningplatform.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<NamedResourceResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public NamedResourceResponse findById(Integer id) {
        return toResponse(getCategory(id));
    }

    @Transactional
    public NamedResourceResponse create(NameRequest request) {
        ensureNameAvailable(request.name(), null);
        return toResponse(categoryRepository.save(new Category(request.name())));
    }

    @Transactional
    public NamedResourceResponse update(Integer id, NameRequest request) {
        Category category = getCategory(id);
        ensureNameAvailable(request.name(), id);
        category.setName(request.name());
        return toResponse(category);
    }

    @Transactional
    public void delete(Integer id) {
        categoryRepository.delete(getCategory(id));
    }

    private Category getCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private void ensureNameAvailable(String name, Integer currentId) {
        boolean exists = currentId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) {
            throw new DuplicateResourceException("Category with this name already exists");
        }
    }

    private NamedResourceResponse toResponse(Category category) {
        return new NamedResourceResponse(category.getId(), category.getName());
    }
}
