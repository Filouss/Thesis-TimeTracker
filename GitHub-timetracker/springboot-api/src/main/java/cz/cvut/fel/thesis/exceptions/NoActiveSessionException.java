package cz.cvut.fel.thesis.exceptions;

/**
 * Signals that a user has no active session when one is required.
 */
public class NoActiveSessionException extends RuntimeException {
    public NoActiveSessionException(String message) {
        super("User does not have an active session");
    }
}
