package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Handles user lookup, creation, and user-specific preferences.
 */
@Service
public class UserService{

    @Autowired
    private UserDAO userDAO;

    /**
     * Finds a user by their GitHub id.
     *
     * @param id GitHub user id
     * @return matching user, or {@code null} when no user exists
     */
    public User getUserByGitHubID(Long id) {
        return userDAO.findByGitHubID(id).orElse(null);
    }

    /**
     * Returns an existing user or creates a new one from GitHub identity data.
     *
     * @param githubId GitHub user id
     * @param username GitHub login name
     * @return existing or newly created user
     */
    public User findOrCreateAndGetUser(Long githubId, String username) {
        User user = getUserByGitHubID(githubId);
        if (user != null) {
            return user;
        }
        user = new User(username, githubId);
        userDAO.save(user);
        return user;
    }

    /**
     * Returns the GitHub issue ids pinned by the given user.
     *
     * @param user application user
     * @return pinned GitHub issue ids
     */
    public Set<Long> getPinnedIssueGitHubIds(User user) {
        return user.getPinnedIssueGithubIds();
    }
}
