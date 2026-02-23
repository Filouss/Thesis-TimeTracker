package cz.cvut.fel.thesis.service;

import cz.cvut.fel.thesis.dao.*;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Repository;
import cz.cvut.fel.thesis.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private RepositoryDAO repositoryDAO;

    @Autowired
    private IssueDAO issueDAO;

    @Autowired
    private TimeBlockDAO timeBlockDAO;

    @Autowired
    private LabelDAO labelDAO;

    @Autowired
    private IssueService issueService;

    public void startSession(int issueNumber, String repo, String owner, User user) {
        GitHubIssueDTO fetchedIssue = issueService.getIssue(issueNumber, repo, owner);

        Repository repository = repositoryDAO
                .findByOwnerAndName(fetchedIssue.owner(), fetchedIssue.repoName())
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setName(fetchedIssue.repoName());
                    newRepo.setOwner(fetchedIssue.owner());
                    repositoryDAO.save(newRepo);
                    return newRepo;
                });


    }

    public void endSession(User user){}
}