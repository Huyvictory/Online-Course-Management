package com.online.course.management.project.utils.courserating;

import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.repository.ICourseRatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CourseRatingServiceUtils {

    private final ICourseRatingRepository courseRatingRepository;

    @Autowired
    public CourseRatingServiceUtils(ICourseRatingRepository courseRatingRepository) {
        this.courseRatingRepository = courseRatingRepository;
    }

    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of("review_text", "rating");

        Set<String> invalidFields = sort.keySet().stream().filter(field -> !validFields.contains(field)).collect(Collectors.toSet());

        if (!invalidFields.isEmpty()) {
            throw new InvalidRequestException(String.format("Invalid sort fields: %s. Valid fields are: %s", String.join(", ", invalidFields), String.join(", ", validFields)));
        }

        // Validate sort directions
        sort.values().forEach(direction -> {
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException(String.format("Invalid sort direction: %s. Must be 'asc' or 'desc'", direction));
            }
        });
    }

    public Sort createCourseRatingSort(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "rating");
        }

        List<Sort.Order> orders = sortParams.entrySet().stream().map(entry -> {
            Sort.Direction direction = entry.getValue().equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            return JpaSort.unsafe(direction, entry.getKey()).getOrderFor(entry.getKey());
        }).collect(Collectors.toList());

        return Sort.by(orders);
    }

    public Map<Long, Long> getRatingDistributionFormatted(Long courseId) {
        Map<Long, Long> formattedDistribution = new LinkedHashMap<>();

        // Get actual distribution
        List<Map<Long, Long>> distribution = courseRatingRepository.getRatingDistribution(courseId);
        distribution.forEach(map -> {
            log.info("{}: {}", map.get("stars").getClass().getName(), map.get("count").getClass().getName());

            formattedDistribution.put(map.get("stars"), map.get("count"));
        });

        return formattedDistribution;
    }

    // Helper method to get percentage distribution
    public Map<Long, Double> getRatingPercentages(Long courseId) {
        Map<Long, Long> distribution = getRatingDistributionFormatted(courseId);
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();

        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        if (total == 0)
            return distribution.keySet().stream().collect(Collectors.toMap(rating -> rating, rating -> 0.0, (a, b) -> b, LinkedHashMap::new));

        return distribution.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> Double.parseDouble(df.format((entry.getValue() * 100.0) / total)), (a, b) -> b, LinkedHashMap::new));
    }
}
