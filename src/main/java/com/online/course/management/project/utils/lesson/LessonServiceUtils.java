package com.online.course.management.project.utils.lesson;

import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.repository.ILessonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LessonServiceUtils {

    private final ILessonRepository lessonRepository;

    @Autowired
    public LessonServiceUtils(ILessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public String hasLessonOrderTakenSingle(Long chapterId, Integer order) {
        if (lessonRepository.isOrderNumberLessonTaken(chapterId, order)) {
            return String.format("Order number %d is already taken in this chapter", order);
        }

        return null;
    }

    public List<String> hasLessonOrderTakenMultiple(Long chapterId, List<Lesson> lessonsList) {

        List<String> takenLessonOrders = new ArrayList<>();
        for (Lesson lesson : lessonsList) {
            var takenLessonOrder = hasLessonOrderTakenSingle(chapterId, lesson.getOrder());

            if (takenLessonOrder != null) {
                takenLessonOrders.add(takenLessonOrder);
            }
        }

        return takenLessonOrders;
    }

    public boolean IsListOrdersContainsDuplicates(List<Integer> listOrders) {
        return listOrders.stream().distinct().count() != listOrders.size();
    }
}
