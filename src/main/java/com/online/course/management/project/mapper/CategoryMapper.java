package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.enums.CourseStatus;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "courseCount", expression = "java(countActiveCourses(category))")
    CategoryDTOs.CategoryResponseDto toDto(Category category);

    Category toEntity(CategoryDTOs.CreateCategoryDTO createCategoryRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDto(CategoryDTOs.UpdateCategoryDTO updateCategoryRequest, @MappingTarget Category category);

    default Long countActiveCourses(Category category) {
        if (category.getCourses() == null) return 0L;
        return category.getCourses().stream()
                .filter(course -> course.getDeletedAt() == null
                        && course.getStatus() != CourseStatus.ARCHIVED)
                .count();
    }

}
