package com.online.course.management.project.controller;

import com.online.course.management.project.constants.ChapterConstants;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.IChapterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
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
}
