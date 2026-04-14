package cz.cvut.fel.thesis.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import cz.cvut.fel.thesis.dto.CommentDTO;
import cz.cvut.fel.thesis.dto.GitHubIssueDTO;
import cz.cvut.fel.thesis.exceptions.UnassignedIssueException;
import cz.cvut.fel.thesis.model.Issue;
import cz.cvut.fel.thesis.model.Session;
import reactor.core.publisher.Mono;

/**
 * Encapsulates GitHub REST API operations used by the application.
 */
@Component
public class GitHubAPIClient {

        @Autowired
        private WebClient github;

        /**
         * Fetches issues assigned to the authenticated GitHub user.
         *
         * @return assigned issues from GitHub
         */
        public List<GitHubIssueDTO> getAssignedIssues() {
                return github.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/issues")
                                .queryParam("filter", "assigned")
                                .queryParam("state", "all")
                                .queryParam("per_page", 100)
                                .build())
                                .retrieve()
                                .onStatus(HttpStatusCode::isError,
                                                resp -> resp.bodyToMono(String.class).map(body -> new RuntimeException(
                                                        "GitHub " + resp.statusCode() + " body: " + body)))
                                .bodyToFlux(GitHubIssueDTO.class)
                                .collectList()
                                .block();
        }

        /**
         * Fetches a specific issue from GitHub.
         *
         * @param issueNumber issue number in the repository
         * @param repo repository name
         * @param owner repository owner
         * @return fetched GitHub issue
         */
        public GitHubIssueDTO getIssue(int issueNumber, String repo, String owner) {
                return github.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/repos/{owner}/{repo}/issues/{issueNumber}")
                                .build(owner, repo, issueNumber))
                        .retrieve()
                        .bodyToMono(GitHubIssueDTO.class)
                        .onErrorMap(WebClientResponseException.NotFound.class, 
                                e -> new UnassignedIssueException(HttpStatus.NOT_FOUND, "Issue not found in GitHub"))
                        .block();
        }

        /**
         * Deletes an existing GitHub issue comment linked to the issue.
         *
         * @param issue issue containing repository data and comment id
         */
        public void deleteComment(Issue issue) {
                github.delete()
                        .uri(uriBuilder -> uriBuilder
                                .path("/repos/{owner}/{repo}/issues/comments/{commentId}")
                                .build(issue.getRepository().getOwner(),
                                        issue.getRepository().getName(),
                                        issue.getGithubCommentId()))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                        resp -> resp.bodyToMono(String.class)
                                                .map(b -> new RuntimeException("GitHub "
                                                        + resp.statusCode() + " body: " + b)))
                        .toBodilessEntity()
                        .block();
        }

        /**
         * Updates an existing GitHub issue comment.
         *
         * @param session session whose issue identifies repository context
         * @param body new comment body
         * @param commentId GitHub comment id
         */
        public void editComment(Session session, String body, Long commentId) {
                github.patch()
                        .uri(uriBuilder -> uriBuilder
                                .path("/repos/{owner}/{repo}/issues/comments/{commentId}")
                                .build(session.getIssue().getRepository().getOwner(),
                                                session.getIssue().getRepository().getName(),
                                                commentId))
                        .bodyValue(Map.of("body", body))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                        resp -> resp.bodyToMono(String.class)
                                                .map(b -> new RuntimeException("GitHub "
                                                        + resp.statusCode() + " body: " + b)))
                        .bodyToMono(CommentDTO.class)
                        .block();
        }

        /**
         * Creates a new comment on a GitHub issue.
         *
         * @param issue issue containing repository and issue identifiers
         * @param body comment body
         * @return created GitHub comment
         */
        public CommentDTO createIssueComment(Issue issue, String body) {
                return github.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/repos/{owner}/{repo}/issues/{issueNumber}/comments")
                                .build(issue.getRepository().getOwner(),
                                        issue.getRepository().getName(),
                                                issue.getIssueNumber()))
                        .bodyValue(Map.of("body", body))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError,
                                        resp -> resp.bodyToMono(String.class)
                                                .map(b -> new RuntimeException("GitHub "
                                                        + resp.statusCode() + " body: " + b)))
                        .bodyToMono(CommentDTO.class)
                        .block();
        }

}
