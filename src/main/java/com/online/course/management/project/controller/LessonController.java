package com.online.course.management.project.controller;

import com.online.course.management.project.constants.LessonConstants;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.ILessonService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping(LessonConstants.UPDATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<LessonDTOs.LessonResponseDto> updateLesson(@PathVariable @Valid long id, @RequestBody @Valid LessonDTOs.UpdateLessonDTO request) {
        LessonDTOs.LessonResponseDto updatedLesson = lessonService.updateLesson(id, request);
        return ResponseEntity.ok(updatedLesson);
    }

    @PutMapping(LessonConstants.BULK_UPDATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<List<LessonDTOs.LessonResponseDto>> bulkUpdateLessons(@RequestBody @Valid LessonDTOs.BulkUpdateLessonDTO request) {
        List<LessonDTOs.LessonResponseDto> updatedLessons = lessonService.bulkUpdateLessons(request);
        return ResponseEntity.ok(updatedLessons);
    }

    @DeleteMapping(LessonConstants.DELETE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> deleteLesson(@PathVariable @Valid long id) {
        lessonService.deleteSingleLesson(id);
        return ResponseEntity.ok("Lesson deleted successfully");
    }

    @DeleteMapping(LessonConstants.BULK_DELETE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> bulkDeleteLessons(@RequestBody @Valid LessonDTOs.BulkOperationLessonDTO request) {
        lessonService.bulkDeleteLessons(request.getLessonIds());
        return ResponseEntity.ok("Lessons deleted successfully");
    }

    @PostMapping(LessonConstants.RESTORE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> restoreLesson(@PathVariable @Valid long id) {
        lessonService.restoreLesson(id);
        return ResponseEntity.ok("Lesson restored successfully");
    }

    @PostMapping(LessonConstants.BULK_RESTORE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> bulkRestoreLessons(@RequestBody @Valid LessonDTOs.BulkOperationLessonDTO request) {
        lessonService.bulkRestoreLessons(request.getLessonIds());
        return ResponseEntity.ok("Lessons restored successfully");
    }

    @PostMapping(LessonConstants.SEARCH_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<LessonDTOs.LessonDetailResponseDto>> searchLessons(
            @Valid @RequestBody LessonDTOs.LessonSearchDTO searchRequest) {

        var lessonsPage = lessonService.searchLessons(searchRequest);

        var response = new PaginationDto.PaginationResponseDto<>(
                lessonsPage.getContent(),
                lessonsPage.getNumber() + 1,
                lessonsPage.getSize(),
                lessonsPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(LessonConstants.REORDER_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> reorderLessons(@PathVariable @Valid long id) {
        lessonService.reorderLessons(id);
        return ResponseEntity.ok("Lessons reordered successfully");
    }
}
