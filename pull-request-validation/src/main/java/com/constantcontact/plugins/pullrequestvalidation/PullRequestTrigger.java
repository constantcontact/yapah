package com.constantcontact.plugins.pullrequestvalidation;

import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.ParametersAction;
import hudson.model.PasswordParameterValue;
import hudson.model.StringParameterValue;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.kohsuke.stapler.DataBoundConstructor;

import antlr.ANTLRException;

public class PullRequestTrigger extends Trigger<AbstractProject<?, ?>> {

  private final String        repositoryName;
  private final String        systemUser;
  private final String        systemUserPassword;
  private final String        repositoryOwner;
  private final String        gitHubRepository;
  private String              sha;
  private String              pullRequestUrl;

  private boolean             isSupposedToRun = false;

  private static final Logger LOGGER          = Logger.getLogger(PullRequestTrigger.class.getName());

  @DataBoundConstructor
  public PullRequestTrigger(String spec, PullRequestTriggerConfig config) throws ANTLRException {
    super(spec);

    this.repositoryName = config.getRepositoryName();
    this.repositoryOwner = config.getRepositoryOwner();
    this.systemUser = config.getSystemUser();
    this.systemUserPassword = config.getSystemUserPassword();
    this.gitHubRepository = config.getGitHubRepository();

  }

  private String getStartDescription(final AbstractProject<?, ?> project) {
    StringBuilder sb = new StringBuilder();
    sb.append("Jenkins Started to Run Tests");
    sb.append("<br />");
    sb.append("<a target='_blank' href='" + project.getAbsoluteUrl()+ "'>");
    sb.append(project.getName());
    sb.append("</a>");
    return sb.toString();
  }

  @Override
  public void run() {

    try {

      GitHubClient githubClient = new GitHubClient("github.roving.com");
      githubClient.setCredentials(getSystemUser(), getSystemUserPassword());

      RepositoryService repositoryService = new RepositoryService(githubClient);
      Repository repository = repositoryService.getRepository(getRepositoryOwner(), getRepositoryName());

      PullRequestService pullRequestService = new PullRequestService(githubClient);
      List<PullRequest> pullRequests = pullRequestService.getPullRequests(repository, "open");

      CommitService commitService = new CommitService(githubClient);

      IssueService issueService = new IssueService(githubClient);

      if (pullRequests.size() == 0) {
        LOGGER.info("Found no pull requests in " + job.getFullDisplayName());
        return;
      }

      for (PullRequest pullRequest : pullRequests) {
        this.sha = pullRequest.getHead().getSha();
        this.pullRequestUrl = pullRequest.getUrl();

        List<Comment> comments = issueService.getComments(getRepositoryOwner(), getRepositoryName(),
            pullRequest.getNumber());

        List<Long> commentIds = new ArrayList<Long>();
        for (Comment comment : comments) {
          commentIds.add(comment.getId());
        }

        if (commentIds.size() == 0) {
          LOGGER.info("Should fire off a trigger, no comments found");

          isSupposedToRun = true;
          createCommentAndCommitStatus(issueService, commitService, repository, pullRequest);
        } else {
          Long mostRecentComment = Collections.max(commentIds);

          for (Comment comment : comments) {
            if (comment.getId() == mostRecentComment) {
              if (!comment.getBody().contains("PR Validator")) {
                LOGGER.info("Should fire off a trigger, no bad comments found");

                isSupposedToRun = true;
                createCommentAndCommitStatus(issueService, commitService, repository, pullRequest);
              }
            }
          }
        }

      }
    } catch (Exception ex) {
      LOGGER.info("Exception occurred stopping the trigger");
      LOGGER.info(ex.getMessage() + "\n" + ex.getStackTrace());
    }

    doRun();

  }

  private void createComment(final IssueService issueService, final Repository repository,
      final PullRequest pullRequest, final String poolingComment) throws Exception {
    issueService.createComment(repository, pullRequest.getNumber(),
        poolingComment);
  }

  private void createCommitStatus(final CommitService commitService, final Repository repository) throws IOException {
    CommitStatus commitStatus = new CommitStatus();
    commitStatus.setDescription(getStartDescription(job));
    commitStatus.setState(CommitStatus.STATE_PENDING);
    commitService.createStatus(repository, sha, commitStatus);
  }

  private void createCommentAndCommitStatus(final IssueService issueService, final CommitService commitService,
      final Repository repository, final PullRequest pullRequest) throws Exception {
    createComment(issueService, repository, pullRequest, getPoolingComment());
    createCommitStatus(commitService, repository);
  }
  
  private String getPoolingComment(){
    StringBuilder sb = new StringBuilder();
    sb.append("<table cellspacing='0' cellpadding='0' ><tr><td align='left'><img src='");
    sb.append(Jenkins.getInstance().getRootUrl());
    sb.append("/favicon.ico' /></td>");
    sb.append("<td>");
    sb.append("QE Jenkins Started to Run Tests against your fork");
    sb.append("<br />");
    sb.append("<a target='_blank' href='" + job.getAbsoluteUrl() + "' title='Click here to view the Jenkins Job for the Fork that the pull request came from'>");
    sb.append("Click here to see Tests Running for " + job.getName());
    sb.append("</a>");
    sb.append("</td>");
    sb.append("</tr></table>");
    return sb.toString();
  }

  public void doRun() {
    try {
      if (isSupposedToRun) {
        PullRequestTriggerConfig expandedConfig = new PullRequestTriggerConfig(systemUser, systemUserPassword,
            repositoryName, repositoryOwner, gitHubRepository, sha, pullRequestUrl);
        List<ParameterValue> stringParams = new ArrayList<ParameterValue>();
        stringParams.add(new StringParameterValue("systemUser", systemUser));
        stringParams.add(new PasswordParameterValue("systemUserPassword", systemUserPassword));
        stringParams.add(new StringParameterValue("repositoryName", repositoryName));
        stringParams.add(new StringParameterValue("repositoryOwner", repositoryOwner));
        stringParams.add(new StringParameterValue("gitHubRepository", gitHubRepository));
        stringParams.add(new StringParameterValue("sha", sha));
        stringParams.add(new StringParameterValue("pullRequestUrl", pullRequestUrl));
        ParametersAction params = new ParametersAction(stringParams);

        job.scheduleBuild2(0, new PullRequestTriggerCause(expandedConfig), params);
      }
    } finally {
      isSupposedToRun = false;
    }
    return;

  }

  @Extension
  public static final class DescriptorImpl extends TriggerDescriptor {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(Item item) {
      return item instanceof BuildableItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return "Github Pull Request Poller";
    }

  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getSystemUser() {
    return systemUser;
  }

  public String getSystemUserPassword() {
    return systemUserPassword;
  }

  public String getRepositoryOwner() {
    return repositoryOwner;
  }

  public String getGitHubRepository() {
    return gitHubRepository;
  }

}
