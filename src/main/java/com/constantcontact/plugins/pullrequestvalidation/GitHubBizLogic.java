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
    private LogWriter logWriter;
    private CommitService commitService;
    private IssueService issueService;
    private Repository repository;

    public GitHubBizLogic(LogWriter logWriter, GitHubClient githubClient, RepositoryService repositoryService,
                          PullRequestService pullRequestService, CommitService commitService, IssueService issueService) {
        this.githubClient = githubClient;
        this.repositoryService = repositoryService;
        this.pullRequestService = pullRequestService;
        this.logWriter = logWriter;
        this.setCommitService(commitService);
        this.setIssueService(issueService);
    }

    private void logGitHubRepo(String repo) {
        logWriter.log(Messages.trigger_logging_1() + repo);
    }

    private void logGitHubURL(String url) {
        logWriter.log(Messages.trigger_logging_2() + url);
    }

    public List<PullRequest> doPreSetup(String systemUser, String systemPassword, String repoOwner, String repoName, String repo,
                                        String githubURL) throws IOException {
        logGitHubRepo(repo);
        logGitHubURL(githubURL);

        return setup(systemUser, systemPassword, repoOwner, repoName, repo);
    }

    private List<PullRequest> setup(String systemUser, String systemPassword, String repoOwner, String repoName, String repo)
            throws IOException {
        githubClient.setCredentials(systemUser, systemPassword);

        setRepository(repositoryService.getRepository(repoOwner, repoName));

        logWriter.log(Messages.trigger_logging_3() + repo);

        return pullRequestService.getPullRequests(repository, "open");
    }

    public void logZeroPR(String repo) {
        logWriter.log(Messages.trigger_logging_4() + repo);
    }

    public void logSHA(String sha) {
        logWriter.log(Messages.trigger_logging_5() + sha);
    }

    public void logPRURL(String url) {
        logWriter.log(Messages.trigger_logging_6() + url);
    }

    public HashMap<Long, Comment> captureComments(String repoOwner, String repoName, PullRequest pullRequest,
                                                  String commentBodyIndicator) throws IOException {
        List<Comment> comments = getIssueService().getComments(repoOwner, repoName, pullRequest.getNumber());
        HashMap<Long, Comment> commentHash = new HashMap<Long, Comment>();
        for (Comment comment : comments) {
            if (comment.getBody().contains(commentBodyIndicator)) {
                commentHash.put(comment.getId(), comment);
            }
        }
        return commentHash;
    }

    public boolean doZeroCommentsWork(boolean shouldRun) {
        boolean shouldRunSetting = shouldRun;
        logWriter.log(Messages.trigger_logging_7());
        shouldRunSetting = true;
        return shouldRunSetting;
    }

    public boolean doNonZeroCommentsWork(boolean shouldRun, HashMap<Long, Comment> commentHash, String sha,
                                         String commentBodyIndicator) throws IOException {
        boolean shouldRunSetting = shouldRun;
        Long mostRecentCommentId = Collections.max(commentHash.keySet());

        Comment mostRecentComment = commentHash.get(mostRecentCommentId);
        List<RepositoryCommit> commits = getCommitService().getCommits(getRepository(), sha, null);
        if (mostRecentComment.getBody().contains(commentBodyIndicator)) {
            for (RepositoryCommit commit : commits) {
                if (commit.getCommit().getAuthor().getDate().after(mostRecentComment.getCreatedAt())) {
                    shouldRunSetting = true;
                    break;
                }
            }
        }
        if (! shouldRunSetting) {
            logWriter.log(Messages.trigger_logging_8());
        }

        return shouldRunSetting;
    }

    public Repository getRepository() {
        return repository;
    }

    protected void setRepository(Repository repository) {
        this.repository = repository;
    }

    protected CommitService getCommitService() {
        return commitService;
    }

    protected void setCommitService(CommitService commitService) {
        this.commitService = commitService;
    }

    protected IssueService getIssueService() {
        return issueService;
    }

    protected void setIssueService(IssueService issueService) {
        this.issueService = issueService;
    }
}
