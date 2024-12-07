package com.online.course.management.project.controller;

import com.online.course.management.project.constants.ChapterConstants;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.IChapterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ChapterConstants.BASE_PATH)
public class ChapterController {

    private final IChapterService chapterService;

    @Autowired
    public ChapterController(IChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @PostMapping(ChapterConstants.CREATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<ChapterDTOs.ChapterDetailResponseDto> createChapter(@RequestBody @Valid ChapterDTOs.CreateChapterDTO request) {

        ChapterDTOs.ChapterDetailResponseDto createdChapter = chapterService.createChapter(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdChapter);

    }

    @PostMapping(ChapterConstants.BULK_CREATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<List<ChapterDTOs.ChapterDetailResponseDto>> bulkCreateChapters(@RequestBody @Valid ChapterDTOs.BulkCreateChapterDTO request) {

        List<ChapterDTOs.ChapterDetailResponseDto> createdChapter = chapterService.bulkCreateChapters(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdChapter);
    }

    @PutMapping(ChapterConstants.UPDATE_DETAILS_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<ChapterDTOs.ChapterDetailResponseDto> updateChapter(@PathVariable @Valid long id, @RequestBody @Valid ChapterDTOs.UpdateChapterDTO request) {
        ChapterDTOs.ChapterDetailResponseDto updatedChapter = chapterService.updateChapter(id, request);
        return ResponseEntity.ok(updatedChapter);
    }

    @PutMapping(ChapterConstants.BULK_UPDATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<List<ChapterDTOs.ChapterResponseDto>> bulkUpdateChapters(@RequestBody @Valid ChapterDTOs.BulkUpdateChapterDTO request) {
        List<ChapterDTOs.ChapterResponseDto> updatedChapters = chapterService.bulkUpdateChapters(request.getChapterIds(), request.getChapters());
        return ResponseEntity.ok(updatedChapters);
    }

    @DeleteMapping(ChapterConstants.DELETE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> deleteChapter(@PathVariable @Valid long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok("Chapter deleted successfully");
    }

    @DeleteMapping(ChapterConstants.BULK_DELETE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> bulkDeleteChapters(@RequestBody @Valid ChapterDTOs.BulkOperationChapterDTO request) {
        chapterService.bulkDeleteChapters(request.getChapterIds());
        return ResponseEntity.ok("Chapters deleted successfully");
    }

    @PostMapping(ChapterConstants.RESTORE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> restoreChapter(@PathVariable @Valid long id) {
        chapterService.restoreChapter(id);
        return ResponseEntity.ok("Chapter restored successfully");
    }

    @PostMapping(ChapterConstants.BULK_RESTORE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> bulkRestoreChapters(@RequestBody @Valid ChapterDTOs.BulkOperationChapterDTO request) {
        chapterService.bulkRestoreChapters(request.getChapterIds());
        return ResponseEntity.ok("Chapters restored successfully");
    }

    @PostMapping(ChapterConstants.GET_DETAILS_PATH)
    public ResponseEntity<ChapterDTOs.ChapterResponseDto> getChapterDetailsById(@PathVariable @Valid long id) {

        ChapterDTOs.ChapterResponseDto response = chapterService.getChapterById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(ChapterConstants.GET_DETAILS_WITH_LESSONS_PATH)
    public ResponseEntity<ChapterDTOs.ChapterDetailResponseDto> getChapterDetailsWithLessonsById(@PathVariable @Valid long id) {
        ChapterDTOs.ChapterDetailResponseDto response = chapterService.getChapterWithLessons(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(ChapterConstants.GET_CHAPTERS_BY_COURSE_PATH)
    public ResponseEntity<List<ChapterDTOs.ChapterResponseDto>> getChaptersByCourseId(@PathVariable @Valid long courseId) {
        List<ChapterDTOs.ChapterResponseDto> response = chapterService.getAllChaptersByCourseId(courseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(ChapterConstants.SEARCH_CHAPTERS_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<ChapterDTOs.ChapterResponseDto>> searchChapters(
            @Valid @RequestBody ChapterDTOs.ChapterSearchDTO searchRequest) {

        var chaptersPage = chapterService.searchChapters(searchRequest, searchRequest.toPageable());

        var response = new PaginationDto.PaginationResponseDto<>(
                chaptersPage.getContent(),
                chaptersPage.getNumber() + 1,
                chaptersPage.getSize(),
                chaptersPage.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @PostMapping(ChapterConstants.CHAPTER_REORDERED_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<String> reorderChapters(@PathVariable @Valid long id) {
        chapterService.reorderChapters(id);
        return ResponseEntity.ok("Chapter reordered successfully");
    }
}
