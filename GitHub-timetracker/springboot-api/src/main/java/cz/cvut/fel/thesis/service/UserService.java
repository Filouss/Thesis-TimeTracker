package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.UserDAO;
import cz.cvut.fel.thesis.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserService{

    @Autowired
    private UserDAO userDAO;

    public User getUserByGitHubID(Long id) {
        return userDAO.findByGitHubID(id).orElse(null);
    }

    public User findOrCreateAndGetUser(Long githubId, String username) {
        User user = getUserByGitHubID(githubId);
        if (user != null) {
            return user;
        }
        user = new User(username, githubId);
        userDAO.save(user);
        return user;
    }

    public Set<Long> getPinnedIssueGitHubIds(User user) {
        return user.getPinnedIssueGithubIds();
    }
}
