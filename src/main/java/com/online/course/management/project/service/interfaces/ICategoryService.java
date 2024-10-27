package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.CategoryDTOs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICategoryService {
    CategoryDTOs.CategoryResponseDto createCategory(CategoryDTOs.CreateCategoryDTO request);

    CategoryDTOs.CategoryResponseDto updateCategory(Long id, CategoryDTOs.UpdateCategoryDTO request);

    void deleteCategory(Long id);

    CategoryDTOs.CategoryResponseDto getCategoryById(Long id);

    boolean isCategoryNameExist(String categoryName);

    Page<CategoryDTOs.CategoryResponseDto> getAllCategories(Pageable pageable);

    Page<CategoryDTOs.CategoryResponseDto> searchCategories(CategoryDTOs.CategorySearchDTO request, Pageable pageable);

    void restoreCategory(Long id);

    boolean isActiveCategory(Long id);
}
