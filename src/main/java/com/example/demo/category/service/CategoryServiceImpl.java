package com.example.demo.category.service;

import com.example.demo.category.dto.CategoryResponse;
import com.example.demo.category.dto.CategorySaveRequest;
import com.example.demo.category.entity.Category;
import com.example.demo.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getAllCategories(Boolean showHidden) {
        return categoryRepository.findAll().stream()
                .filter(c -> (showHidden != null && showHidden) || (c.getIsActive() == null || c.getIsActive()))
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        return CategoryResponse.fromEntity(category);
    }

    @Override
    public CategoryResponse createCategory(CategorySaveRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .image(request.getImage())
                .build();
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, CategorySaveRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        category.setName(request.getName());
        if (request.getImage() != null && !request.getImage().isBlank()) {
            category.setImage(request.getImage());
        }
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        category.setIsActive(category.getIsActive() == null || !category.getIsActive());
        categoryRepository.save(category);
    }
}
