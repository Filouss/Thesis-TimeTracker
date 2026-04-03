package cz.cvut.fel.thesis.exceptions;

/**
 * Signals that a user could not be found for the supplied GitHub id.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long githubId) {
        super("User not found for githubId=" + githubId);
    }
}
