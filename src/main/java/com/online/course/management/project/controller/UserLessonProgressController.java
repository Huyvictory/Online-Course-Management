package com.online.course.management.project.controller;

import com.online.course.management.project.constants.UserLessonProgressConstants;
import com.online.course.management.project.dto.UserLessonProgressDtos;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.IUserLessonProgressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(UserLessonProgressConstants.BASE_PATH)
public class UserLessonProgressController {

    private final IUserLessonProgressService userLessonProgressService;

    @Autowired
    public UserLessonProgressController(IUserLessonProgressService userLessonProgressService) {
        this.userLessonProgressService = userLessonProgressService;
    }

    @PostMapping(UserLessonProgressConstants.START_LEARNING_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR", "USER"})
    public ResponseEntity<UserLessonProgressDtos.LessonProgressResponseDTO> startLearningLesson(@Valid @RequestBody UserLessonProgressDtos.UpdateStatusLessonProgressDTO request) {
        var response = userLessonProgressService.startLearningLesson(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(UserLessonProgressConstants.COMPLETE_LEARNING_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR", "USER"})
    public ResponseEntity<UserLessonProgressDtos.LessonProgressResponseDTO> completeLearningLesson(@Valid @RequestBody UserLessonProgressDtos.UpdateStatusLessonProgressDTO request) {

        var response = userLessonProgressService.completeLearningLesson(request);

        return ResponseEntity.ok(response);
    }
}
