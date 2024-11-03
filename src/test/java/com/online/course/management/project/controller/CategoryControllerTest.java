package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.CategoryDTOs;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.repository.ICategoryRepository;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;


import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICategoryService categoryService;

    @MockBean
    private ICategoryRepository categoryRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private CategoryDTOs.CategoryResponseDto mockCategoryResponse;
    private CategoryDTOs.CreateCategoryDTO createCategoryRequest;
    private CategoryDTOs.UpdateCategoryDTO updateCategoryRequest;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        setupMockData();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupMockData() {
        // Setup mock category response
        mockCategoryResponse = new CategoryDTOs.CategoryResponseDto();
        mockCategoryResponse.setId(1L);
        mockCategoryResponse.setName("Test Category");
        mockCategoryResponse.setCourseCount(0L);
        mockCategoryResponse.setCreatedAt(LocalDateTime.now());
        mockCategoryResponse.setUpdatedAt(LocalDateTime.now());

        // Setup create category request
        createCategoryRequest = new CategoryDTOs.CreateCategoryDTO();
        createCategoryRequest.setName("New Category");

        // Setup update category request
        updateCategoryRequest = new CategoryDTOs.UpdateCategoryDTO();
        updateCategoryRequest.setName("Updated Category");
    }

    @Test
    @WithMockCustomUser()
    void verifyControllerRegistration() {
        assertNotNull(webApplicationContext.getBean(CategoryController.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN", "USER"})
    void createCategory_Success() throws Exception {
        when(categoryService.createCategory(any(CategoryDTOs.CreateCategoryDTO.class)))
                .thenReturn(mockCategoryResponse);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(mockCategoryResponse.getId()))
                .andExpect(jsonPath("$.name").value(mockCategoryResponse.getName()));

        verify(categoryService).createCategory(any());
    }

    @Test
    void createCategory_Unauthorized() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User is not authenticated"))
                .andReturn();

        System.out.println("Unauthorized Response Status: " + result.getResponse().getContentAsString());
        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @WithMockCustomUser(roles = "USER", username = "user")
    void createCategory_Forbidden() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCategoryRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User does not have the required role"))
                .andReturn();

        System.out.println("Forbidden Response Status: " + result.getResponse().getStatus());
        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN", "USER"})
    void updateCategory_Success() throws Exception {
        when(categoryService.updateCategory(eq(1L), any())).thenReturn(mockCategoryResponse);

        mockCategoryResponse.setName("Updated Category");

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCategoryRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockCategoryResponse.getId()))
                .andExpect(jsonPath("$.name").value(updateCategoryRequest.getName()));

        verify(categoryService).updateCategory(eq(1L), any(CategoryDTOs.UpdateCategoryDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN", "USER"})
    void updateCategory_NotFound() throws Exception {
        when(categoryService.updateCategory(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(put("/api/v1/categories/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCategoryRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found"));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN", "USER"})
    void deleteCategory_Success() throws Exception {
        doNothing().when(categoryService).deleteCategory(999L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    @WithMockCustomUser()
    void deleteCategory_NotFound() throws Exception {

        doThrow(new ResourceNotFoundException("Category not found")).when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Category not found"));

        verify(categoryService, never()).deleteCategory(999L);
        verify(categoryRepository, never()).softDeleteCategory(1L);

    }

    @Test
    @WithMockCustomUser()
    void deleteCategory_AlreadyDeleted() throws Exception {
        doThrow(new ForbiddenException("Cannot delete archived category")).when(categoryService).deleteCategory(1L);

        mockCategoryResponse.setDeletedAt(LocalDateTime.now().plusDays(1));

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Cannot delete archived category"));

        verify(categoryRepository, never()).softDeleteCategory(1L);
    }


    @Test
    @WithMockCustomUser()
    void restoreCategory_Success() throws Exception {
        doNothing().when(categoryService).restoreCategory(1L);
        doNothing().when(categoryRepository).restoreCategory(1L);

        mockMvc.perform(post("/api/v1/categories/1/restore"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored category successfully"));


        verify(categoryService).restoreCategory(1L);
    }

    @Test
    void getCategoryById_Success() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(mockCategoryResponse);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mockCategoryResponse.getId()))
                .andExpect(jsonPath("$.name").value(mockCategoryResponse.getName()));

        verify(categoryService).getCategoryById(1L);
    }

    @Test
    void searchCategories_Success() throws Exception {
        // Arrange
        CategoryDTOs.CategorySearchDTO searchRequest = new CategoryDTOs.CategorySearchDTO();
        searchRequest.setName("Test");
        searchRequest.setArchived(false);

        List<CategoryDTOs.CategoryResponseDto> categories = Collections.singletonList(mockCategoryResponse);

        // Create PageRequest with the same pagination parameters as the request
        PageRequest pageRequest = PageRequest.of(searchRequest.getPage() - 1, searchRequest.getLimit());

        // Create Page with the correct pagination information
        Page<CategoryDTOs.CategoryResponseDto> categoryPage = new PageImpl<>(
                categories,
                pageRequest,
                categories.size()
        );

        when(categoryService.searchCategories(any(), any()))
                .thenReturn(categoryPage);

        log.info("Mocking searchCategories with request: {}", searchRequest);

        // Act
        mockMvc.perform(post("/api/v1/categories/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(mockCategoryResponse.getId()))
                .andExpect(jsonPath("$.data[0].name").value(mockCategoryResponse.getName()))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));

        // Assert
        verify(categoryService).searchCategories(any(), any());
    }

    @Test
    void searchCategories_InvalidRequest() throws Exception {
        CategoryDTOs.CategorySearchDTO invalidRequest = new CategoryDTOs.CategorySearchDTO();
        invalidRequest.setPage(0); // Invalid page number

        mockMvc.perform(post("/api/v1/categories/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("page: Page index must not be less than one"));

        verify(categoryService, never()).searchCategories(any(), any());
    }
}