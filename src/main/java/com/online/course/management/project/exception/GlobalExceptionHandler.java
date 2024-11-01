package com.online.course.management.project.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.online.course.management.project.dto.ErrorResponseDTO;
import com.online.course.management.project.exception.business.*;
import com.online.course.management.project.exception.business.account.AccountException;
import com.online.course.management.project.exception.business.account.EmailAlreadyExistsException;
import com.online.course.management.project.exception.business.account.WrongEmailPasswordException;
import com.online.course.management.project.exception.technical.DatabaseException;
import com.online.course.management.project.exception.technical.ExternalServiceException;
import com.online.course.management.project.exception.technical.TechnicalException;
import com.online.course.management.project.utils.exception.GlobalExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException ex) {
        log.warn("Business exception occurred: {}", ex.getMessage());
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof UnauthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof ForbiddenException) {
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof AccountException) {
            if (ex instanceof EmailAlreadyExistsException) {
                status = HttpStatus.CONFLICT;
            } else if (ex instanceof WrongEmailPasswordException) {
                status = HttpStatus.UNAUTHORIZED;
            }

        }
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(ex.getMessage(), status.value());
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponseDTO> handleTechnicalException(TechnicalException ex) {
        log.error("Technical exception occurred: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An internal error occurred";

        if (ex instanceof DatabaseException) {
            message = "A database error occurred";
        } else if (ex instanceof ExternalServiceException) {
            message = "An error occurred with an external service";
            status = HttpStatus.BAD_GATEWAY;
        }

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(message, status.value());
        return new ResponseEntity<>(errorResponse, status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponseDTO errorResponse = new ErrorResponseDTO("Validation failed", HttpStatus.BAD_REQUEST.value(), errors);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO("Authentication failed", HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument exception occurred: {}", ex.getMessage());
        ErrorResponseDTO errorResponse = new ErrorResponseDTO(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponseDTO errorResponse = new ErrorResponseDTO("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message;
        if (ex.getCause() instanceof UnrecognizedPropertyException unrecognizedProp) {
            // Handle unknown field error
            message = String.format(
                    "Unknown field: '%s'. Allowed fields are: %s",
                    unrecognizedProp.getPropertyName(),
                    String.join(", ", GlobalExceptionUtils.getKnownProperties(new HashSet<>(unrecognizedProp.getKnownPropertyIds())))
            );
        } else if (ex.getCause() instanceof InvalidFormatException invalidFormat) {
            // Handle enum parsing errors
            if (invalidFormat.getTargetType() != null && invalidFormat.getTargetType().isEnum()) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    message = String.format("Invalid value for enum field: %s",
                            objectMapper.writeValueAsString(Map.of(
                                    "field", invalidFormat.getPath().get(0).getFieldName(),
                                    "invalidValue", invalidFormat.getValue(),
                                    "allowedValues", Arrays.stream(invalidFormat.getTargetType().getEnumConstants())
                                            .map(Object::toString)
                                            .collect(Collectors.toList())
                            ))
                    );
                } catch (JsonProcessingException e) {
                    message = "Invalid request body. Please check your JSON format and field types.";
                }
            } else {
                // Handle other format errors
                message = String.format("Invalid value for field '%s': %s",
                        invalidFormat.getPath().get(0).getFieldName(),
                        invalidFormat.getValue());
            }
        } else {
            // Handle other JSON parsing errors
            message = "Invalid request body. Please check your JSON format and field types.";
        }

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                message,
                HttpStatus.BAD_REQUEST.value()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


}
