package com.online.course.management.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;


public class PaginationDto {

    @Data
    public static class PaginationRequestDto {

        @Min(value = 1, message = "Page index must not be less than one")
        private int page = 1;

        @Min(value = 1, message = "Limit must be greater than or equal to 1")
        @Max(value = 50, message = "Limit must be less than or equal to 50")
        private int limit = 10;

        public Pageable toPageable() {
            return PageRequest.of(page - 1, limit);
        }
    }


    @Data
    @AllArgsConstructor
    public static class PaginationResponseDto<T> {
        private List<T> data;
        private int currentPage;
        private int limit;
        private long total;

    }
}
