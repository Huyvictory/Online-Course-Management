package com.online.course.management.project.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

public class CommonResponseDTOs {

    @Data
    public static class DeleteSuccessfullyDTO {
        private String message;
        private HttpStatus status = HttpStatus.NO_CONTENT;

        public DeleteSuccessfullyDTO(
                String message) {
            this.message = message;
        }
    }
}
