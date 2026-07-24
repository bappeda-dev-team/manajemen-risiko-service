package cc.kertaskerja.manrisk.exception;

import cc.kertaskerja.manrisk.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    // ========== 400 BAD REQUEST EXCEPTIONS ==========

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
          BadRequestException ex, HttpServletRequest request) {

        logger.warning("Bad Request Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
          MethodArgumentNotValidException ex, HttpServletRequest request) {

        logger.warning("Validation Exception: " + ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              validationErrors,
              "Validation failed"
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
          ConstraintViolationException ex, HttpServletRequest request) {

        logger.warning("Constraint Violation Exception: " + ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        for (ConstraintViolation<?> violation : violations) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            validationErrors.put(fieldName, errorMessage);
        }

        ApiResponse<Map<String, String>> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              validationErrors,
              "Constraint validation failed"
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
          HttpMessageNotReadableException ex, HttpServletRequest request) {

        logger.warning("HTTP Message Not Readable Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              "Invalid JSON format or malformed request body"
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatchException(
          MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        logger.warning("Method Argument Type Mismatch Exception: " + ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
              ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              message
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestParameterException(
          MissingServletRequestParameterException ex, HttpServletRequest request) {

        logger.warning("Missing Servlet Request Parameter Exception: " + ex.getMessage());

        String message = String.format("Required parameter '%s' of type '%s' is missing",
              ex.getParameterName(), ex.getParameterType());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              message
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
          DataIntegrityViolationException ex, HttpServletRequest request) {

        logger.warning("Data Integrity Violation Exception: " + ex.getMessage());

        String message = "Data integrity violation. This operation conflicts with existing data constraints.";
        if (ex.getMessage().contains("unique")) {
            message = "Duplicate entry. A record with this information already exists.";
        }

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.BAD_REQUEST.value(),
              message
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ========== 401 UNAUTHORIZED EXCEPTIONS ==========

    @ExceptionHandler(UnauthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthenticationException(
          UnauthenticationException ex, HttpServletRequest request) {

        logger.warning("Unauthorized Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.UNAUTHORIZED.value(),
              ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // ========== 403 FORBIDDEN EXCEPTIONS ==========
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbiddenException(
          ForbiddenException ex, HttpServletRequest request) {

        logger.warning("Forbidden Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.FORBIDDEN.value(), // 403
              ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }


    // ========== 404 NOT FOUND EXCEPTIONS ==========

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
          ResourceNotFoundException ex, HttpServletRequest request) {

        logger.warning("Resource not found at " + request.getRequestURI() + ": " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.NOT_FOUND.value(),
              ex.getMessage(),
              null
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // ========== 409 CONFLICT EXCEPTIONS ==========

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflictException(
          ConflictException ex, HttpServletRequest request) {

        logger.warning("Conflict data at: " + request.getRequestURI() + ": " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.CONFLICT.value(),
              ex.getMessage(),
              null
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // ========== 429 RATE LIMIT EXCEPTIONS ==========

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimitException(
          RateLimitException ex, HttpServletRequest request) {

        logger.warning("Rate limit exceeded at: " + request.getRequestURI() + ": " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              429, // kode HTTP yang sesuai
              ex.getMessage(),
              null
        );

        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    // ========== 500 INTERNAL SERVER ERROR EXCEPTIONS ==========

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ApiResponse<Object>> handleInternalServerException(
          InternalServerException ex, HttpServletRequest request) {

        logger.severe("Internal Server Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
          DataAccessException ex, HttpServletRequest request) {

        logger.severe("Data Access Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "Database operation failed. Please try again later."
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(
          NullPointerException ex, HttpServletRequest request) {

        logger.severe("Null Pointer Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "An unexpected error occurred. Please contact support."
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
          RuntimeException ex, HttpServletRequest request) {

        logger.severe("Runtime Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "An unexpected runtime error occurred: " + ex.getMessage()
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
          Exception ex, HttpServletRequest request) {

        logger.severe("Generic Exception: " + ex.getMessage());

        ApiResponse<Object> response = ApiResponse.error(
              HttpStatus.INTERNAL_SERVER_ERROR.value(),
              "An unexpected error occurred. Please contact support if the problem persists."
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

