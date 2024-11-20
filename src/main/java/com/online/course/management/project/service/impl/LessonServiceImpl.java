package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Lesson;
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

import java.util.List;

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
}
