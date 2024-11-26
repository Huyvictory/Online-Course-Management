package com.online.course.management.project.repository;

import com.online.course.management.project.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IUserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    @Override
    <S extends UserLessonProgress> S save(S userLessonProgress);
}