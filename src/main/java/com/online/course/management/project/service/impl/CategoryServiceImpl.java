package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.mapper.CategoryMapper;
import com.online.course.management.project.repository.ICategoryRepository;
import com.online.course.management.project.service.interfaces.ICategoryService;
import com.online.course.management.project.utils.category.CategoryServiceUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@Slf4j
public class CategoryServiceImpl implements ICategoryService {

    private final ICategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final CategoryServiceUtils categoryServiceUtils;

    @Autowired
    public CategoryServiceImpl(ICategoryRepository categoryRepository, CategoryMapper categoryMapper, CategoryServiceUtils categoryServiceUtils) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.categoryServiceUtils = categoryServiceUtils;
    }

    @Override
    @Transactional
    public CategoryDTOs.CategoryResponseDto createCategory(CategoryDTOs.CreateCategoryDTO request) {
        log.info("Creating new category with name: {}", request.getName());

        if (isCategoryNameExist(request.getName())) {
            throw new IllegalArgumentException("Category with name already exists");
        }

        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDTOs.CategoryResponseDto updateCategory(Long id, CategoryDTOs.UpdateCategoryDTO request) {
        log.info("Updating category with id: {}", id);


        if (isCategoryNameExist(request.getName())) {
            throw new IllegalArgumentException("Category with name already exists");
        }

        Category category = categoryServiceUtils.getCategoryOrThrow(id);

        categoryMapper.updateCategoryFromDto(request, category);

        Category updatedCategory = categoryRepository.save(category);

        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public void deleteCategory(Long id) {
        log.info("Soft deleting category with id: {}", id);

        Category category = categoryServiceUtils.getCategoryOrThrow(id);

        if (category.getDeletedAt() != null) {
            throw new ForbiddenException("Cannot delete archived category");
        }

        categoryRepository.softDeleteCategory(id);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryDTOs.CategoryResponseDto getCategoryById(Long id) {
        log.info("Fetching category with id: {}", id);

        return categoryMapper.toDto(categoryServiceUtils.getCategoryOrThrow(id));
    }

    @Override
    public boolean isCategoryNameExist(String categoryName) {
        return categoryRepository.existsByName(categoryName);
    }


    @Override
    public Page<CategoryDTOs.CategoryResponseDto> searchCategories(CategoryDTOs.CategorySearchDTO request, Pageable pageable) {
        log.info("Searching categories with criteria: {}", request);

        Page<Category> categories = categoryRepository.searchCategories(
                request.getName(),
                request.getFromDate(),
                request.getToDate(),
                request.getArchived(),
                pageable
        );

        long totalCount = categoryRepository.countCategories(
                request.getName(),
                request.getFromDate(),
                request.getToDate(),
                request.getArchived()
        );

        return new PageImpl<>(
                categories.getContent().stream()
                        .map(categoryMapper::toDto)
                        .collect(Collectors.toList()),
                pageable,
                totalCount
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public void restoreCategory(Long id) {
        log.info("Restoring category with id: {}", id);

        Category category = categoryServiceUtils.getCategoryOrThrow(id);

        if (category.getDeletedAt() == null) {
            throw new IllegalArgumentException("Category is not soft deleted");
        }

        categoryRepository.restoreCategory(id);
    }
}
