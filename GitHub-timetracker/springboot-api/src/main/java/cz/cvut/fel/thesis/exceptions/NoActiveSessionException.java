package cz.cvut.fel.thesis.exceptions;

public class NoActiveSessionException extends RuntimeException {
    public NoActiveSessionException(String message) {
        super("User does not have an active session");
    }
}
