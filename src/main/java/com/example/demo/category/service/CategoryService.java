package com.example.demo.category.service;

import com.example.demo.category.dto.CategoryResponse;
import com.example.demo.category.dto.CategorySaveRequest;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(Boolean showHidden);
    default List<CategoryResponse> getAllCategories() {
        return getAllCategories(false);
    }
    CategoryResponse getCategoryById(Integer id);
    CategoryResponse createCategory(CategorySaveRequest request);
    CategoryResponse updateCategory(Integer id, CategorySaveRequest request);
    void deleteCategory(Integer id);
}
