package com.online.course.management.project.controller;

import com.online.course.management.project.constants.LessonConstants;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.ILessonService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(LessonConstants.BASE_PATH)
public class LessonController {

    private final ILessonService lessonService;

    @Autowired
    public LessonController(ILessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping(LessonConstants.CREATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<LessonDTOs.LessonResponseDto> createLesson(@RequestBody @Valid LessonDTOs.CreateLessonDTOWithChapterId request) {

        LessonDTOs.LessonResponseDto createdLesson = lessonService.createLesson(request);
        return ResponseEntity.status(201).body(createdLesson);
    }

    @PostMapping(LessonConstants.BULK_CREATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<List<LessonDTOs.LessonResponseDto>> bulkCreateLessons(@RequestBody @Valid LessonDTOs.BulkCreateLessonDTO request) {

        List<LessonDTOs.LessonResponseDto> createdLessons = lessonService.bulkCreateLessons(request);
        return ResponseEntity.status(201).body(createdLessons);
    }
}
