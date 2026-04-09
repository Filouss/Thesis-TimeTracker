package cz.cvut.fel.thesis.configuration;

import cz.cvut.fel.thesis.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.NotActiveException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized exception handling for REST controllers.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Converts {@link UserNotFoundException} into an HTTP 404 response payload.
         *
         * @param ex raised exception
         * @return response entity with error details
         */
    @ExceptionHandler
    public ResponseEntity<Object> handleUserNotFound(UserNotFoundException ex){
        return buildResponse(HttpStatus.NOT_FOUND, "User not found", ex.getMessage());
    }

        /**
         * Converts {@link NotActiveException} into an HTTP 400 response payload.
         *
         * @param ex raised exception
         * @return response entity with error details
         */
    @ExceptionHandler
    public ResponseEntity<Object> handleNotActiveSession(NotActiveException ex){
        return buildResponse(HttpStatus.BAD_REQUEST, "No session active", ex.getMessage());
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(jakarta.persistence.EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Entity not found", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error", 
            "An unexpected error occurred. Please try again later."
        );
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }

}
