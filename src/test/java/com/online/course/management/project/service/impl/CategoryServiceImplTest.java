package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CategoryMapper;
import com.online.course.management.project.repository.ICategoryRepository;
import com.online.course.management.project.utils.category.CategoryServiceUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class CategoryServiceImplTest {

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private CategoryServiceUtils categoryServiceUtils;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryDTOs.CategoryResponseDto testCategoryResponseDto;
    private CategoryDTOs.CreateCategoryDTO createCategoryDTO;
    private CategoryDTOs.UpdateCategoryDTO updateCategoryDTO;

    @BeforeEach
    void setUp() {
        // Initialize test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setUpdatedAt(LocalDateTime.now());
        testCategory.setCourses(new HashSet<>());

        // Initialize test category response DTO
        testCategoryResponseDto = new CategoryDTOs.CategoryResponseDto();
        testCategoryResponseDto.setId(1L);
        testCategoryResponseDto.setName("Test Category");
        testCategoryResponseDto.setCreatedAt(LocalDateTime.now());
        testCategoryResponseDto.setUpdatedAt(LocalDateTime.now());
        testCategoryResponseDto.setCourseCount(0L);

        // Initialize create category DTO
        createCategoryDTO = new CategoryDTOs.CreateCategoryDTO();
        createCategoryDTO.setName("New Category");

        // Initialize update category DTO
        updateCategoryDTO = new CategoryDTOs.UpdateCategoryDTO();
        updateCategoryDTO.setName("Updated Category");
    }

    @Test
    void createCategory_Success() {
        // Arrange
        when(categoryRepository.existsByName(any())).thenReturn(false);
        when(categoryMapper.toEntity(any())).thenReturn(testCategory);
        when(categoryRepository.save(any())).thenReturn(testCategory);
        when(categoryMapper.toDto(any())).thenReturn(testCategoryResponseDto);

        // Act
        CategoryDTOs.CategoryResponseDto result = categoryService.createCategory(createCategoryDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryResponseDto.getId(), result.getId());
        assertEquals(testCategoryResponseDto.getName(), result.getName());

        verify(categoryRepository).existsByName(createCategoryDTO.getName());
        verify(categoryMapper).toEntity(createCategoryDTO);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryMapper).toDto(any(Category.class));
    }

    @Test
    void createCategory_DuplicateName() {
        // Arrange
        when(categoryRepository.existsByName(any())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.createCategory(createCategoryDTO),
                "Category with name already exists");

        verify(categoryRepository).existsByName(createCategoryDTO.getName());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateCategory_Success() {
        // Arrange
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);
        when(categoryRepository.existsByName(anyString())).thenReturn(false);
        when(categoryRepository.save(any())).thenReturn(testCategory);
        when(categoryMapper.toDto(any())).thenReturn(testCategoryResponseDto);

        // Act
        CategoryDTOs.CategoryResponseDto result = categoryService.updateCategory(1L, updateCategoryDTO);

        // Assert
        assertNotNull(result);
        verify(categoryServiceUtils).getCategoryOrThrow(1L);
        verify(categoryRepository).existsByName(updateCategoryDTO.getName());
        verify(categoryMapper).updateCategoryFromDto(updateCategoryDTO, testCategory);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void updateCategory_CategoryNotFound() {
        // Arrange
        when(categoryServiceUtils.getCategoryOrThrow(anyLong()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(1L, updateCategoryDTO));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_Success() {
        // Arrange
        testCategory.setDeletedAt(null);
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);

        // Act
        categoryService.deleteCategory(1L);

        // Assert
        verify(categoryServiceUtils).getCategoryOrThrow(1L);
        verify(categoryRepository).softDeleteCategory(1L);
    }

    @Test
    void deleteCategory_AlreadyDeleted() {
        // Arrange
        testCategory.setDeletedAt(LocalDateTime.now());
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);

        // Act & Assert
        assertThrows(ForbiddenException.class,
                () -> categoryService.deleteCategory(1L));

        verify(categoryRepository, never()).softDeleteCategory(anyLong());
    }

    @Test
    void restoreCategory_Success() {
        // Arrange
        testCategory.setDeletedAt(LocalDateTime.now());
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);

        // Act
        categoryService.restoreCategory(1L);

        // Assert
        verify(categoryServiceUtils).getCategoryOrThrow(1L);
        verify(categoryRepository).restoreCategory(1L);
    }

    @Test
    void restoreCategory_NotDeleted() {
        // Arrange
        testCategory.setDeletedAt(null);
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> categoryService.restoreCategory(1L));

        verify(categoryRepository, never()).restoreCategory(anyLong());
    }

    @Test
    void searchCategories_Success() {
        // Arrange
        List<Category> categories = Collections.singletonList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);
        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .name("Test")
                .archived(false)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        when(categoryRepository.searchCategories(
                eq("Test"),
                any(),
                any(),
                eq(false),
                any(Pageable.class)
        )).thenReturn(categoryPage);
        when(categoryRepository.countCategories(
                eq("Test"),
                any(),
                any(),
                eq(false)
        )).thenReturn(1L);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(testCategoryResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCategoryResponseDto.getName(), result.getContent().get(0).getName());

        verify(categoryRepository).searchCategories(
                eq("Test"),
                any(),
                any(),
                eq(false),
                any(Pageable.class)
        );

        verify(categoryRepository).countCategories(
                eq("Test"),
                any(),
                any(),
                eq(false)
        );

        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    void searchCategories_WithDateRange_Success() {
        // Arrange
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime toDate = LocalDateTime.now().plusMonths(1);

        // Use existing testCategory and create an additional one
        Category additionalCategory = new Category();
        additionalCategory.setId(2L);
        additionalCategory.setName("Additional Category");
        additionalCategory.setCreatedAt(LocalDateTime.now());
        additionalCategory.setUpdatedAt(LocalDateTime.now());
        additionalCategory.setCourses(new HashSet<>());

        // Create DTO for additional category based on existing DTO pattern
        CategoryDTOs.CategoryResponseDto additionalResponseDto = new CategoryDTOs.CategoryResponseDto();
        additionalResponseDto.setId(2L);
        additionalResponseDto.setName("Additional Category");
        additionalResponseDto.setCreatedAt(additionalCategory.getCreatedAt());
        additionalResponseDto.setUpdatedAt(additionalCategory.getUpdatedAt());
        additionalResponseDto.setCourseCount(0L);

        List<Category> categories = Arrays.asList(testCategory, additionalCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .archived(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Mock repository behavior
        when(categoryRepository.searchCategories(
                isNull(),
                eq(fromDate),
                eq(toDate),
                eq(false),
                any(Pageable.class)
        )).thenReturn(categoryPage);

        when(categoryRepository.countCategories(
                isNull(),
                eq(fromDate),
                eq(toDate),
                eq(false)
        )).thenReturn(2L);

        // Use existing mapper mock for testCategory and add mapping for additional category
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryResponseDto);
        when(categoryMapper.toDto(additionalCategory)).thenReturn(additionalResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream()
                .map(CategoryDTOs.CategoryResponseDto::getName)
                .collect(Collectors.toSet())
                .containsAll(Arrays.asList("Test Category", "Additional Category")));
    }

    @Test
    void searchCategories_WithOnlyFromDate_Success() {
        // Arrange
        LocalDateTime fromDate = LocalDateTime.now().plusMonths(1);

        List<Category> categories = Collections.singletonList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .fromDate(fromDate)
                .archived(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Mock repository behavior
        when(categoryRepository.searchCategories(
                isNull(),
                eq(fromDate),
                any(),
                eq(false),
                any(Pageable.class)
        )).thenReturn(categoryPage);

        when(categoryRepository.countCategories(
                isNull(),
                eq(fromDate),
                any(),
                eq(false)
        )).thenReturn(1L);

        // Test mapper behavior
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCategoryResponseDto.getName(), result.getContent().get(0).getName());

        verify(categoryRepository).searchCategories(
                isNull(),
                eq(fromDate),
                any(),
                eq(false),
                any(Pageable.class)
        );

        verify(categoryRepository).countCategories(
                isNull(),
                eq(fromDate),
                any(),
                eq(false)
        );

        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    void searchCategories_WithOnlyToDate_Success() {
        // Arrange
        LocalDateTime toDate = LocalDateTime.now().minusMonths(1);

        List<Category> categories = Collections.singletonList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .toDate(toDate)
                .archived(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Mock repository behavior
        when(categoryRepository.searchCategories(
                isNull(),
                any(),
                eq(toDate),
                eq(false),
                any(Pageable.class)
        )).thenReturn(categoryPage);

        when(categoryRepository.countCategories(
                isNull(),
                any(),
                eq(toDate),
                eq(false)
        )).thenReturn(1L);

        // Test mapper behavior
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCategoryResponseDto.getName(), result.getContent().get(0).getName());

        verify(categoryRepository).searchCategories(
                isNull(),
                any(),
                eq(toDate),
                eq(false),
                any(Pageable.class)
        );

        verify(categoryRepository).countCategories(
                isNull(),
                any(),
                eq(toDate),
                eq(false)
        );

        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    void searchCategories_NameSearch_Success() {
        // Arrange
        String name = "Test";

        List<Category> categories = Collections.singletonList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .name(name)
                .archived(false)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Mock repository behavior
        when(categoryRepository.searchCategories(
                eq(name),
                any(),
                any(),
                eq(false),
                any(Pageable.class)
        )).thenReturn(categoryPage);

        when(categoryRepository.countCategories(
                eq(name),
                any(),
                any(),
                eq(false)
        )).thenReturn(1L);

        // Test mapper behavior
        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getName().contains(name));

        verify(categoryRepository).searchCategories(
                eq(name),
                any(),
                any(),
                eq(false),
                any(Pageable.class)
        );

        verify(categoryRepository).countCategories(
                eq(name),
                any(),
                any(),
                eq(false)
        );

        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    void searchCategories_IncludeArchived_Success() {

        testCategory.setDeletedAt(LocalDateTime.now().minusMonths(1));
        testCategoryResponseDto.setDeletedAt(testCategory.getDeletedAt());

        // Arrange
        List<Category> categories = Collections.singletonList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories);

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .archived(true)
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Mock repository behavior
        when(categoryRepository.searchCategories(
                isNull(),
                any(),
                any(),
                eq(true),
                any(Pageable.class)
        )).thenReturn(categoryPage);

        when(categoryRepository.countCategories(
                isNull(),
                any(),
                any(),
                eq(true)
        )).thenReturn(1L);

        when(categoryMapper.toDto(testCategory)).thenReturn(testCategoryResponseDto);

        // Act
        Page<CategoryDTOs.CategoryResponseDto> result = categoryService.searchCategories(searchDTO, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertNotNull(result.getContent().get(0).getDeletedAt());

        verify(categoryRepository).searchCategories(
                isNull(),
                any(),
                any(),
                eq(true),
                any(Pageable.class)
        );

        verify(categoryRepository).countCategories(
                isNull(),
                any(),
                any(),
                eq(true)
        );
    }

    @Test
    void searchCategories_InvalidDateRange_ThrowsException() {
        // Arrange
        LocalDateTime fromDate = LocalDateTime.of(2024, 12, 31, 0, 0);
        LocalDateTime toDate = LocalDateTime.of(2024, 1, 1, 0, 0);  // Before fromDate

        CategoryDTOs.CategorySearchDTO searchDTO = CategoryDTOs.CategorySearchDTO.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();


        log.info("searchDTO: {}", searchDTO);

        // Act & Assert
        // Create validator
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // Act
        Set<ConstraintViolation<CategoryDTOs.CategorySearchDTO>> violations = validator.validate(searchDTO);

        // Assert
        assertFalse(violations.isEmpty(), "Violations should not be empty");
        ConstraintViolation<CategoryDTOs.CategorySearchDTO> violation = violations.iterator().next();
        assertEquals("toDate must be after fromDate", violation.getMessage());

        // Verify repository was never called
        verify(categoryRepository, never()).searchCategories(any(), any(), any(), any(), any());
        verify(categoryRepository, never()).countCategories(any(), any(), any(), any());
    }

    @Test
    void getCategoryById_Success() {
        // Arrange
        when(categoryServiceUtils.getCategoryOrThrow(anyLong())).thenReturn(testCategory);
        when(categoryMapper.toDto(any(Category.class))).thenReturn(testCategoryResponseDto);

        // Act
        CategoryDTOs.CategoryResponseDto result = categoryService.getCategoryById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testCategoryResponseDto.getId(), result.getId());
        assertEquals(testCategoryResponseDto.getName(), result.getName());

        verify(categoryServiceUtils).getCategoryOrThrow(1L);
        verify(categoryMapper).toDto(testCategory);
    }

    @Test
    void getCategoryById_NotFound() {
        // Arrange
        when(categoryServiceUtils.getCategoryOrThrow(anyLong()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(1L));

        verify(categoryMapper, never()).toDto(any());
    }
}
