package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "courseCount", ignore = true)
        // This will be set manually as it requires a count query
    CategoryDTOs.CategoryResponseDto toDto(Category category);

    Category toEntity(CategoryDTOs.CreateCategoryDTO createCategoryRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDto(CategoryDTOs.UpdateCategoryDTO updateCategoryRequest, @MappingTarget Category category);

}
