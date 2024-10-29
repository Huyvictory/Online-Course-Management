package com.online.course.management.project.utils.exception;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GlobalExceptionUtils {
    public static List<String> getKnownProperties(Set<?> knownPropertyIds) {
        if (knownPropertyIds == null) {
            return List.of();
        }
        return knownPropertyIds.stream()
                .map(Object::toString)
                .sorted()
                .collect(Collectors.toList());
    }
}
