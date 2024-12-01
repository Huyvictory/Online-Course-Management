package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserLessonProgressDtos;
import com.online.course.management.project.entity.UserLessonProgress;
import com.online.course.management.project.enums.ProgressStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.UserLessonProgressMapper;
import com.online.course.management.project.repository.IUserLessonProgressRepository;
import com.online.course.management.project.service.interfaces.IUserLessonProgressService;
import com.online.course.management.project.utils.user.UserSecurityUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserLessonProgressImpl implements IUserLessonProgressService {

    private final IUserLessonProgressRepository userLessonProgressRepository;
    private final UserLessonProgressMapper userLessonProgressMapper;
    private final UserSecurityUtils userSecurityUtils;

    @Autowired
    public UserLessonProgressImpl(IUserLessonProgressRepository userLessonProgressRepository, UserLessonProgressMapper userLessonProgressMapper, UserSecurityUtils userSecurityUtils) {
        this.userLessonProgressRepository = userLessonProgressRepository;
        this.userLessonProgressMapper = userLessonProgressMapper;
        this.userSecurityUtils = userSecurityUtils;
    }


    @Override
    @Transactional
    public UserLessonProgressDtos.LessonProgressResponseDTO startLearningLesson(UserLessonProgressDtos.UpdateStatusLessonProgressDTO request) {

        UserLessonProgress userLessonProgressToStart = userLessonProgressRepository.
                findById(request.getId()).orElseThrow(() -> new RuntimeException("Lesson progress not found"));

        if (!userSecurityUtils.isAdmin() && !userLessonProgressToStart.getUser().getId().equals(userSecurityUtils.getCurrentUser().getId())) {
            throw new InvalidRequestException("You don't have permission to start this lesson");
        }

        if (userLessonProgressToStart.getStatus() == ProgressStatus.IN_PROGRESS) {
            throw new InvalidRequestException("Lesson is already started");
        }

        if (userLessonProgressToStart.getStatus() == ProgressStatus.COMPLETED) {
            throw new InvalidRequestException("Lesson is already completed");
        }

        userLessonProgressToStart.setStatus(ProgressStatus.IN_PROGRESS);
        userLessonProgressToStart.setLastAccessedAt(LocalDateTime.now());

        UserLessonProgress savedUserLessonProgress = userLessonProgressRepository.save(userLessonProgressToStart);

        return userLessonProgressMapper.toDto(savedUserLessonProgress);
    }

    @Override
    @Transactional
    public UserLessonProgressDtos.LessonProgressResponseDTO completeLearningLesson(UserLessonProgressDtos.UpdateStatusLessonProgressDTO request) {
        UserLessonProgress userLessonProgressToStart = userLessonProgressRepository.
                findById(request.getId()).orElseThrow(() -> new RuntimeException("Lesson progress not found"));

        if (!userSecurityUtils.isAdmin() && !userLessonProgressToStart.getUser().getId().equals(userSecurityUtils.getCurrentUser().getId())) {
            throw new InvalidRequestException("You don't have permission to complete this lesson");
        }

        if (userLessonProgressToStart.getStatus() == ProgressStatus.COMPLETED) {
            throw new InvalidRequestException("Lesson is already completed");
        }

        if (userLessonProgressToStart.getStatus() != ProgressStatus.IN_PROGRESS) {
            throw new InvalidRequestException("You must start the lesson before completing it");
        }

        userLessonProgressToStart.setStatus(ProgressStatus.COMPLETED);
        userLessonProgressToStart.setLastAccessedAt(LocalDateTime.now());
        userLessonProgressToStart.setCompletionDate(LocalDateTime.now());

        UserLessonProgress savedUserLessonProgress = userLessonProgressRepository.save(userLessonProgressToStart);

        return userLessonProgressMapper.toDto(savedUserLessonProgress);
    }
}
