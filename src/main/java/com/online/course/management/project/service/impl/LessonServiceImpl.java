package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.LessonMapper;
import com.online.course.management.project.repository.ILessonRepository;
import com.online.course.management.project.service.interfaces.ILessonService;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.lesson.LessonServiceUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class LessonServiceImpl implements ILessonService {

    private final ILessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final LessonServiceUtils lessonServiceUtils;
    private final ChapterServiceUtils chapterServiceUtils;

    @Autowired
    public LessonServiceImpl(ILessonRepository lessonRepository, LessonMapper lessonMapper, LessonServiceUtils lessonServiceUtils, ChapterServiceUtils chapterServiceUtils) {
        this.lessonRepository = lessonRepository;
        this.lessonMapper = lessonMapper;
        this.lessonServiceUtils = lessonServiceUtils;
        this.chapterServiceUtils = chapterServiceUtils;
    }


    @Override
    @Transactional
    public LessonDTOs.LessonResponseDto createLesson(LessonDTOs.CreateLessonDTOWithChapterId request) {

        // Check if chapter exists
        Chapter chapter = chapterServiceUtils.getChapterOrThrow(request.getChapterId());

        // Check chapter's accessibility
        chapterServiceUtils.validateChapterAccess(chapter);

        // Check order of lesson
        var takenLessonOrder = lessonServiceUtils.hasLessonOrderTakenSingle(chapter.getId(), request.getOrder());

        if (takenLessonOrder != null) {
            throw new InvalidRequestException(
                    takenLessonOrder
            );
        }

        Lesson lessonToCreate = lessonMapper.toEntity(request);

        lessonToCreate.setChapter(chapter);

        Lesson savedLesson = lessonRepository.save(lessonToCreate);
        return lessonMapper.toDto(savedLesson);
    }

    @Override
    @Transactional
    public List<LessonDTOs.LessonResponseDto> bulkCreateLessons(LessonDTOs.BulkCreateLessonDTO request) {

        // Check if chapter exists
        Chapter chapter = chapterServiceUtils.getChapterOrThrow(request.getChapterId());

        // Check chapter's accessibility
        chapterServiceUtils.validateChapterAccess(chapter);

        List<LessonDTOs.CreateLessonDTO> listLessonsPayload = request.getLessons();

        List<Lesson> lessons = listLessonsPayload.stream().map(lessonMapper::toEntity).toList();

        List<String> takenLessonOrders = lessonServiceUtils.hasLessonOrderTakenMultiple(request.getChapterId(), lessons);

        // Check order of lessons
        if (!takenLessonOrders.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("One or more lesson orders have been taken in this chapter: %s",
                            String.join(", ", takenLessonOrders)
                    )
            );
        }

        // Check if lessons list contains duplicates
        if (lessonServiceUtils.IsListOrdersContainsDuplicates(lessons.stream().map(Lesson::getOrder).toList())) {
            throw new InvalidRequestException("Duplicate lesson order found");
        }

        chapter.setLessons(lessons);

        List<Lesson> savedLessons = lessonRepository.saveAll(lessons);

        return savedLessons.stream().map(lessonMapper::toDto).toList();
    }

    @Override
    @Transactional
    public LessonDTOs.LessonResponseDto updateLesson(Long id, LessonDTOs.UpdateLessonDTO request) {

        Lesson lessonToUpdate = lessonServiceUtils.GetLessonOrThrow(id);

        // Validate accessibility
        chapterServiceUtils.validateChapterAccess(lessonToUpdate.getChapter());

        // Validate order
        if (request.getOrder() != null) {
            var takenLessonOrder = lessonServiceUtils.hasLessonOrderTakenSingle(lessonToUpdate.getChapter().getId(), request.getOrder());

            if (takenLessonOrder != null) {
                throw new InvalidRequestException(
                        takenLessonOrder
                );
            }
        }

        // Validate status
        if (request.getStatus() != null) {
            try {
                lessonServiceUtils.validateLessonStatus(lessonToUpdate.getStatus(), request.getStatus());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            if (request.getStatus() == CourseStatus.ARCHIVED) {
                lessonToUpdate.setDeletedAt(LocalDateTime.now());
            } else {
                lessonToUpdate.setDeletedAt(null);
            }
        }

        lessonMapper.updateLessonFromDto(request, lessonToUpdate);

        Lesson savedLesson = lessonRepository.save(lessonToUpdate);
        return lessonMapper.toDto(savedLesson);
    }

    @Override
    @Transactional
    public List<LessonDTOs.LessonResponseDto> bulkUpdateLessons(LessonDTOs.BulkUpdateLessonDTO request) {

        if (request.getLessonIds().size() != request.getLessons().size()) {
            throw new InvalidRequestException("Number of lesson IDs and update requests must match");
        }

        // Validate number of allowed lessons per request
        lessonServiceUtils.validateBulkOperation(request.getLessonIds());

        List<LessonDTOs.UpdateLessonDTO> listLessonsPayload = request.getLessons();
        List<Lesson> lessonsToUpdate = lessonRepository.findAllById(request.getLessonIds());

        // Validate accessibility
        lessonsToUpdate.forEach(lesson -> chapterServiceUtils.validateChapterAccess(lesson.getChapter()));

        // Validate order
        List<Integer> payloadOrders = request.getLessons().stream().map(LessonDTOs.UpdateLessonDTO::getOrder).filter(Objects::nonNull).toList();

        if (!payloadOrders.isEmpty() && lessonServiceUtils.IsListOrdersContainsDuplicates(payloadOrders)) {
            throw new InvalidRequestException("Duplicate lesson order found");
        }

        List<String> takenLessonOrders = new ArrayList<>();
        for (Integer order : payloadOrders) {
            var takenLessonOrder = lessonServiceUtils.hasLessonOrderTakenSingle(lessonsToUpdate.get(0).getChapter().getId(), order);
            if (takenLessonOrder != null) {
                takenLessonOrders.add(takenLessonOrder);
            }
        }

        if (!takenLessonOrders.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("One or more lesson orders have been taken in this chapter: %s",
                            String.join(", ", takenLessonOrders)
                    )
            );
        }

        // Validate status
        for (int i = 0; i < lessonsToUpdate.size(); i++) {
            LessonDTOs.UpdateLessonDTO lessonDTOPayload = listLessonsPayload.get(i);
            if (lessonDTOPayload.getStatus() != null) {

                try {
                    lessonServiceUtils.validateLessonStatus(lessonsToUpdate.get(i).getStatus(), lessonDTOPayload.getStatus());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                if (lessonDTOPayload.getStatus() == CourseStatus.ARCHIVED) {
                    lessonsToUpdate.get(i).setDeletedAt(LocalDateTime.now());
                } else {
                    lessonsToUpdate.get(i).setDeletedAt(null);
                }
            }
        }

        // Map update info
        for (int i = 0; i < lessonsToUpdate.size(); i++) {
            LessonDTOs.UpdateLessonDTO lessonDTOPayload = listLessonsPayload.get(i);
            lessonMapper.updateLessonFromDto(lessonDTOPayload, lessonsToUpdate.get(i));
        }

        List<Lesson> savedLessons = lessonRepository.saveAll(lessonsToUpdate);

        return savedLessons.stream().map(lessonMapper::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteSingleLesson(Long id) {
        log.info("Deleting lesson with ID: {}", id);

        Lesson lessonToDelete = lessonServiceUtils.GetLessonOrThrow(id);
        chapterServiceUtils.validateChapterAccess(lessonToDelete.getChapter());

        if (lessonToDelete.getDeletedAt() != null) {
            throw new InvalidRequestException("Lesson is already deleted");
        }

        lessonRepository.batchSoftDeleteLessons(List.of(id));

        log.info("Deleted lesson with ID: {}", id);
    }

    @Override
    @Transactional
    public void bulkDeleteLessons(List<Long> ids) {
        log.info("Bulk deleting {} lessons", ids.size());

        List<Lesson> lessonsToDelete = lessonRepository.findAllById(ids);

        lessonServiceUtils.validateBulkOperation(ids);

        for (Lesson lesson : lessonsToDelete) {
            chapterServiceUtils.validateChapterAccess(lesson.getChapter());
        }

        lessonRepository.batchSoftDeleteLessons(ids);

        log.info("Successfully deleted {} lessons", ids.size());
    }

    @Override
    @Transactional
    public void restoreLesson(Long id) {
        log.info("Restoring lesson with ID: {}", id);

        Lesson lessonToRestore = lessonServiceUtils.GetLessonOrThrow(id);
        chapterServiceUtils.validateChapterAccess(lessonToRestore.getChapter());

        if (lessonToRestore.getDeletedAt() == null) {
            throw new InvalidRequestException("Lesson is not deleted");
        }

        lessonRepository.batchRestoreLessons(List.of(id));

        log.info("Restored lesson with ID: {}", id);
    }

    @Override
    @Transactional
    public void bulkRestoreLessons(List<Long> ids) {
        log.info("Bulk restoring {} lessons", ids.size());

        List<Lesson> lessonsToRestore = lessonRepository.findAllById(ids);

        lessonServiceUtils.validateBulkOperation(ids);

        for (Lesson lesson : lessonsToRestore) {
            chapterServiceUtils.validateChapterAccess(lesson.getChapter());
        }

        lessonRepository.batchRestoreLessons(ids);

        log.info("Successfully restored {} lessons", ids.size());
    }
}
