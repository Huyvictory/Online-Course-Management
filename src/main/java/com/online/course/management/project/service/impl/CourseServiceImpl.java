package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CourseMapper;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.service.interfaces.ICourseService;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseServiceImpl implements ICourseService {

    private final ICourseRepository courseRepository;
    private final IUserRepository userRepository;
    private final CourseMapper courseMapper;
    private final CourseServiceUtils courseServiceUtils;


    @Autowired
    public CourseServiceImpl(
            ICourseRepository courseRepository,
            IUserRepository userRepository,
            CourseMapper courseMapper,
            CourseServiceUtils courseServiceUtils) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseMapper = courseMapper;
        this.courseServiceUtils = courseServiceUtils;
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseDTOS.CourseDetailsResponseDto createCourse(CourseDTOS.CreateCourseRequestDTO request) {
        log.info("Creating new course with title: {}", request.getTitle());

        Course course = courseMapper.toEntity(request);

        if (request.getInstructorId() != null) {
            User instructor = courseServiceUtils.determineInstructor(request.getInstructorId());
            course.setInstructor(instructor);
        }

        // Important: Initialize the categories set if null
        if (course.getCategories() == null) {
            course.setCategories(new HashSet<>());
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = courseServiceUtils.validateCategories(request.getCategoryIds());
            for (Category category : categories) {
                course.addCategory(category);
            }
        }

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDto(savedCourse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public CourseDTOS.CourseDetailsResponseDto updateCourse(Long id, CourseDTOS.UpdateCourseRequestDTO request) {
        log.info("Updating course with id: {}", id);

        Course course = courseServiceUtils.getCourseWithValidation(id);

        if (request.getInstructorId() != null) {
            User newInstructor = courseServiceUtils.determineInstructor(request.getInstructorId());
            course.setInstructor(newInstructor);
        }

        // If status is being updated, validate the transition
        if (request.getStatus() != null) {
            try {
                courseServiceUtils.validateCourseStatus(course.getStatus(), request.getStatus());
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException("Error processing status validation");
            }

            // Handle archived status and deletedAt
            if (request.getStatus() == CourseStatus.ARCHIVED) {
                course.setDeletedAt(LocalDateTime.now());
            } else if (course.getStatus() == CourseStatus.ARCHIVED) {
                // If transitioning from ARCHIVED to another status, clear deletedAt
                course.setDeletedAt(null);
            }
        }

        courseMapper.updateCourseFromDto(request, course);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            courseServiceUtils.updateCourseCategories(course, request.getCategoryIds());
        }

        Course updatedCourse = courseRepository.save(course);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public void archiveCourse(Long id) {
        log.info("Archiving course with id: {}", id);
        courseServiceUtils.getCourseWithValidation(id);
        courseRepository.archiveCourse(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public void unarchiveCourse(Long id) {
        log.info("Unarchiving course with id: {}", id);
        courseServiceUtils.getCourseWithValidation(id);
        courseRepository.unarchiveCourse(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courses", key = "#id")
    public CourseDTOS.CourseDetailsResponseDto getCourseById(Long id) {
        log.info("Fetching course with id: {}", id);
        Course course = courseServiceUtils.GetCourseWithoutValidation(id);
        return courseMapper.toDto(course);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> searchCourses(
            CourseDTOS.SearchCourseRequestDTO request,
            Pageable pageable) {
        log.info("Searching courses with criteria: {}", request);

        // Validate and create sort if provided in request
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            courseServiceUtils.validateSortFields(request.getSort());
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    courseServiceUtils.createSort(request.getSort())
            );
        } else {
            // Use default sort if none provided
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    courseServiceUtils.handleCreateDefaultSort()
            );
        }

        return courseRepository.searchCourses(
                request.getTitle(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getInstructorName(),
                request.getFromDate(),
                request.getToDate(),
                request.getCategoryIds(),
                request.getIncludeArchived(),
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByInstructor(
            Long instructorId,
            boolean includeArchived,
            Pageable pageable) {
        log.info("Fetching courses for instructor: {}, includeArchived: {}",
                instructorId, includeArchived);

        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instructor not found");
        }

        pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                courseServiceUtils.handleCreateDefaultSort()
        );

        return courseRepository.findByInstructorId(instructorId, includeArchived, pageable)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByStatus(
            CourseStatus status,
            Pageable pageable) {
        log.info("Fetching courses with status: {}", status);

        // Use default sort if none provided

        pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                courseServiceUtils.handleCreateDefaultSort()
        );

        return courseRepository.findByStatus(status.name(), pageable)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "latestCourses", key = "#limit")
    public List<CourseDTOS.CourseDetailsResponseDto> getLatestCourses(int limit) {
        log.info("Fetching latest {} courses", limit);
        return courseRepository.findLatestCourses(limit)
                .stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }
}