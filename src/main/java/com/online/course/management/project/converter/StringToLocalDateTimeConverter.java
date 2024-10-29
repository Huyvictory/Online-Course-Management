package com.online.course.management.project.converter;

import com.online.course.management.project.exception.business.InvalidRequestException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

@Component
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
    // Format for full date-time
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // More lenient format for simple date that allows single digits
    private static final DateTimeFormatter LENIENT_DATE_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-")
                    .appendPattern("M-")    // Single M allows single-digit months
                    .appendPattern("d")     // Single d allows single-digit days
                    .toFormatter();

    @Override
    public LocalDateTime convert(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }

        try {
            // Check if it's a simple date format (no time component)
            if (!source.contains("T")) {
                LocalDate date = LocalDate.parse(source, LENIENT_DATE_FORMATTER);
                return date.atStartOfDay();
            }

            // Try parsing as full date time
            return LocalDateTime.parse(source, DATE_TIME_FORMATTER);

        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(
                    "Invalid date format. Supported formats: " +
                            "yyyy-MM-dd'T'HH:mm:ss.SSS (e.g., 2024-08-27T17:29:01.187) or " +
                            "yyyy-M-d (e.g., 2024-8-27 or 2024-08-27)"
            );
        }
    }
}
