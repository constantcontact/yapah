package com.constantcontact.plugins.pullrequestvalidation;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class is the business logic wrapper around the github client api
 */
public class GitHubBizLogic {

    private GitHubClient githubClient;
    private RepositoryService repositoryService;
    private PullRequestService pullRequestService;
    private Logger logger;
    private CommitService commitService;
    private IssueService issueService;
    private PullRequestTriggerConfig config;

    public GitHubBizLogic(Logger logger, GitHubClient githubClient, RepositoryService repositoryService,
                          PullRequestService pullRequestService, CommitService commitService, IssueService issueService,
                          PullRequestTriggerConfig config) {
        this.githubClient = githubClient;
        this.repositoryService = repositoryService;
        this.pullRequestService = pullRequestService;
        this.logger = logger;
        this.commitService = commitService;
        this.issueService = issueService;
        this.config = config;
    }

    protected void logGitHubRepo(String repo) {
        logger.log(Messages.trigger_logging_1() + repo);
    }

    protected void logGitHubURL(String url) {
        logger.log(Messages.trigger_logging_2() + url);
    }

    protected List<PullRequest> setup(String systemUser, String systemPassword, String repoOwner, String repoName, String repo)
            throws IOException {
        githubClient.setCredentials(systemUser, systemPassword);

        Repository repository = repositoryService.getRepository(repoOwner, repoName);

        logger.log(Messages.trigger_logging_3() + repo);

        return pullRequestService.getPullRequests(repository, "open");
    }

    protected void logZeroPR(String repo) {
        logger.log(Messages.trigger_logging_4() + repo);
    }

    protected void logSHA(String sha) {
        logger.log(Messages.trigger_logging_5() + sha);
    }

    protected void logPRURL(String url) {
        logger.log(Messages.trigger_logging_6() + url);
    }

    protected HashMap<Long, Comment> captureComments(PullRequest pullRequest, String commentBodyIndicator) throws IOException {
        List<Comment> comments = issueService.getComments(config.getRepositoryOwner(), config.getRepositoryName(), pullRequest.getNumber());
        HashMap<Long, Comment> commentHash = new HashMap<Long, Comment>();
        for (Comment comment : comments) {
            if (comment.getBody().contains(commentBodyIndicator)) {
                commentHash.put(comment.getId(), comment);
            }
        }
        return commentHash;
    }

    protected boolean doZeroCommentsWork(boolean shouldRun) {
        boolean shouldRunSetting = shouldRun;
        logger.log(Messages.trigger_logging_7());
        shouldRunSetting = true;
        return shouldRunSetting;
    }

    protected boolean doNonZeroCommentsWork(boolean shouldRun, HashMap<Long, Comment> commentHash, Repository repository, String sha,
                                            String commentBodyIndicator) throws IOException {
        boolean shouldRunSetting = shouldRun;
        Long mostRecentCommentId = Collections.max(commentHash.keySet());

        Comment mostRecentComment = commentHash.get(mostRecentCommentId);
        List<RepositoryCommit> commits = commitService.getCommits(repository, sha, null);
        if (mostRecentComment.getBody().contains(commentBodyIndicator)) {
            for (RepositoryCommit commit : commits) {
                if (commit.getCommit().getAuthor().getDate().after(mostRecentComment.getCreatedAt())) {
                    shouldRunSetting = true;
                }
            }
        }
        if (! shouldRunSetting) {
            logger.log(Messages.trigger_logging_8());
        }

        return shouldRunSetting;
    }
}
