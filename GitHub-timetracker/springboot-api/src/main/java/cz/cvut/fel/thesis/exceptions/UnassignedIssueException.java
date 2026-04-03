package cz.cvut.fel.thesis.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Signals that an operation was attempted on an issue not assigned to the user.
 */
public class UnassignedIssueException extends RuntimeException {
    public UnassignedIssueException(HttpStatus badRequest, String message) {
        super(message);
    }
}
