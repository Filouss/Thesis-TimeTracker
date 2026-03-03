package cz.cvut.fel.thesis.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long githubId) {
        super("User not found for githubId=" + githubId);
    }
}
