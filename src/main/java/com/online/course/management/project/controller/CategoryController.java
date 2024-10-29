package com.online.course.management.project.controller;

import com.online.course.management.project.constants.CategoryConstants;
import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.dto.CommonResponseDTOs;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.ICategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CategoryConstants.BASE_PATH)
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;

    @Autowired
    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @RequiredRole({"ADMIN"})
    public ResponseEntity<CategoryDTOs.CategoryResponseDto> createCategory(@Valid @RequestBody CategoryDTOs.CreateCategoryDTO request) {
        log.info("Creating new category with name: {}", request.getName());

        CategoryDTOs.CategoryResponseDto response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(CategoryConstants.PATH_VARIABLE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<CategoryDTOs.CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTOs.UpdateCategoryDTO request) {
        log.info("Updating category with id: {}", id);
        CategoryDTOs.CategoryResponseDto response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(CategoryConstants.PATH_VARIABLE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<CommonResponseDTOs.DeleteSuccessfullyDTO> deleteCategory(@PathVariable Long id) {
        log.info("Deleting category with id: {}", id);
        categoryService.deleteCategory(id);

        CommonResponseDTOs.DeleteSuccessfullyDTO response = new CommonResponseDTOs.DeleteSuccessfullyDTO("Deleted category with id: " + id);
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    @PostMapping(CategoryConstants.RESTORE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<CategoryDTOs.RestoreCategoryResponseDTO> restoreCategory(@PathVariable Long id) {
        log.info("Restoring category with id: {}", id);
        categoryService.restoreCategory(id);

        CategoryDTOs.RestoreCategoryResponseDTO response = new CategoryDTOs.RestoreCategoryResponseDTO("Restored category successfully");

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(CategoryConstants.PATH_VARIABLE_PATH)
    public ResponseEntity<CategoryDTOs.CategoryResponseDto> getCategoryById(@PathVariable Long id) {
        log.info("Fetching category with id: {}", id);
        CategoryDTOs.CategoryResponseDto response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(CategoryConstants.SEARCH_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<CategoryDTOs.CategoryResponseDto>> searchCategories(
            @Valid @RequestBody CategoryDTOs.CategorySearchDTO searchRequest) {
        log.info("Searching categories with criteria: {}", searchRequest);

        var categoriesPage = categoryService.searchCategories(CategoryDTOs.CategorySearchDTO.builder()
                .name(searchRequest.getName())
                .archived(searchRequest.getArchived())
                .fromDate(searchRequest.getFromDate())
                .toDate(searchRequest.getToDate())
                .build(), searchRequest.toPageable());

        var response = new PaginationDto.PaginationResponseDto<>(
                categoriesPage.getContent(),
                categoriesPage.getNumber() + 1,
                categoriesPage.getSize(),
                categoriesPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }
}
