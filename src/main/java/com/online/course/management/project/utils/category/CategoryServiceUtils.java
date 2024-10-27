package com.online.course.management.project.utils.category;

import com.online.course.management.project.entity.Category;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.ICategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CategoryServiceUtils {

    private final ICategoryRepository categoryRepository;

    public CategoryServiceUtils(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }
}
