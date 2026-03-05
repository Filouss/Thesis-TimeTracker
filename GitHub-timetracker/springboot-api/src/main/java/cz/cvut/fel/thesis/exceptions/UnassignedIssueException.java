package cz.cvut.fel.thesis.exceptions;

import org.springframework.http.HttpStatus;

public class UnassignedIssueException extends RuntimeException {
    public UnassignedIssueException(HttpStatus badRequest, String message) {
        super(message);
    }
}
