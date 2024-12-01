package com.online.course.management.project.utils.courserating;

import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.repository.ICourseRatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CourseRatingServiceUtils {

    private final ICourseRatingRepository courseRatingRepository;

    @Autowired
    public CourseRatingServiceUtils(ICourseRatingRepository courseRatingRepository) {
        this.courseRatingRepository = courseRatingRepository;
    }

    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "review_text",
                "rating"
        );

        Set<String> invalidFields = sort.keySet().stream()
                .filter(field -> !validFields.contains(field))
                .collect(Collectors.toSet());

        if (!invalidFields.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("Invalid sort fields: %s. Valid fields are: %s",
                            String.join(", ", invalidFields),
                            String.join(", ", validFields))
            );
        }

        // Validate sort directions
        sort.values().forEach(direction -> {
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException(
                        String.format("Invalid sort direction: %s. Must be 'asc' or 'desc'", direction)
                );
            }
        });
    }

    public Sort createCourseRatingSort(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "rating");
        }

        List<Sort.Order> orders = sortParams.entrySet().stream()
                .map(entry -> {
                    Sort.Direction direction = entry.getValue().equalsIgnoreCase("asc") ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
                    return JpaSort.unsafe(direction, entry.getKey()).getOrderFor(entry.getKey());
                })
                .collect(Collectors.toList());

        return Sort.by(orders);
    }
}
