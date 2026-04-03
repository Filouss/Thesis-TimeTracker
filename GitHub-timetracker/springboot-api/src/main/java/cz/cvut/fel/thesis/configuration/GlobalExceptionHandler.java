package cz.cvut.fel.thesis.configuration;

import cz.cvut.fel.thesis.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.NotActiveException;
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
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "User not found",
                        "message", ex.getMessage(),
                        "status", 404
                ));
    }

        /**
         * Converts {@link NotActiveException} into an HTTP 400 response payload.
         *
         * @param ex raised exception
         * @return response entity with error details
         */
    @ExceptionHandler
    public ResponseEntity<Object> handleNotActiveSession(NotActiveException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "No session active for this user",
                        "message", ex.getMessage(),
                        "status", 400
                ));
    }

}
