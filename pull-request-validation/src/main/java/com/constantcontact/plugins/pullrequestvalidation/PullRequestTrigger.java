package com.constantcontact.plugins.pullrequestvalidation;

import static hudson.Util.fixNull;
import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.PasswordParameterValue;
import hudson.model.Project;
import hudson.model.StringParameterValue;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import jenkins.model.Jenkins;

import org.apache.commons.jelly.XMLOutput;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.ANTLRException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

public class PullRequestTrigger extends Trigger<AbstractProject<?, ?>> {

  private final String                              repositoryName;
  private final String                              systemUser;
  private final String                              systemUserPassword;
  private final String                              repositoryOwner;
  private final String                              gitHubRepository;
  private String                                    sha;
  private String                                    pullRequestUrl;

  private boolean                                   isSupposedToRun = false;

  private static final Logger                       LOGGER          = LoggerFactory.getLogger(PullRequestTrigger.class);
  private static final String                       PR_VALIDATOR    = "~PR_VALIDATOR";

  private final ArrayList<PullRequestTriggerConfig> additionalConfigs;

  @DataBoundConstructor
  public PullRequestTrigger(String spec, List<PullRequestTriggerConfig> configs) throws ANTLRException {
    super(spec);

    ArrayList<PullRequestTriggerConfig> configsCopy = new ArrayList<PullRequestTriggerConfig>(
        fixNull(configs));

    PullRequestTriggerConfig firstConfig;

    if (configsCopy.isEmpty()) {
      firstConfig = new PullRequestTriggerConfig("", null, null, null, null, null, null);
    } else {
      firstConfig = configsCopy.remove(0);
    }

    this.repositoryName = firstConfig.getRepositoryName();
    this.repositoryOwner = firstConfig.getRepositoryOwner();
    this.systemUser = firstConfig.getSystemUser();
    this.systemUserPassword = firstConfig.getSystemUserPassword();
    this.gitHubRepository = firstConfig.getGitHubRepository();

    this.additionalConfigs = configsCopy;

  }

  @SuppressWarnings("unused")
  // called reflectively by XStream
  private PullRequestTrigger() {
    this.repositoryName = null;
    this.repositoryOwner = "";
    this.systemUser = "";
    this.systemUserPassword = "";
    this.gitHubRepository = "";
    this.additionalConfigs = null;
  }

  public List<PullRequestTriggerConfig> getConfigs() {
    ImmutableList.Builder<PullRequestTriggerConfig> builder = ImmutableList
        .builder();
    builder.add(new PullRequestTriggerConfig(systemUser, systemUserPassword, repositoryName, repositoryOwner,
        gitHubRepository, sha, pullRequestUrl));
    if (additionalConfigs != null) {
      builder.addAll(additionalConfigs);
    }
    return builder.build();
  }

  private String getStartDescription(final AbstractProject<?, ?> project) {
    StringBuilder sb = new StringBuilder();
    sb.append("Jenkins Started to Run Tests at");
    sb.append(project.getName());
    return sb.toString();
  }

  @Override
  public void run() {
    StreamTaskListener listener;
    try {
      listener = new StreamTaskListener(getLogFile());

      PrintStream logger = listener.getLogger();
      for (PullRequestTriggerConfig config : getConfigs()) {
        if (null == config.getGitHubRepository()) {
          return;
        }
        try {
          this.sha = null;
          this.pullRequestUrl = null;

          logger.println("Polling for " + config.getGitHubRepository());

          logger.println("Instantiating Clients");
          GitHubClient githubClient = new GitHubClient("github.roving.com");
          githubClient.setCredentials(config.getSystemUser(), config.getSystemUserPassword());

          RepositoryService repositoryService = new RepositoryService(githubClient);
          Repository repository = repositoryService
              .getRepository(config.getRepositoryOwner(), config.getRepositoryName());

          PullRequestService pullRequestService = new PullRequestService(githubClient);
          logger.println("Gathering Pull Requests for " + config.getGitHubRepository());

          List<PullRequest> pullRequests = pullRequestService.getPullRequests(repository, "open");
          CommitService commitService = new CommitService(githubClient);
          IssueService issueService = new IssueService(githubClient);

          if (pullRequests.size() == 0) {
            logger.println("Found no Pull Requests for " + config.getGitHubRepository());
            continue;
          }

          for (PullRequest pullRequest : pullRequests) {
            this.sha = pullRequest.getHead().getSha();
            logger.println("Got SHA1 : " + this.sha);

            this.pullRequestUrl = pullRequest.getUrl();
            logger.println("Pull Request URL : " + this.pullRequestUrl);

            List<Comment> comments = issueService.getComments(config.getRepositoryOwner(), config.getRepositoryName(),
                pullRequest.getNumber());

            HashMap<Long, Comment> commentHash = new HashMap<Long, Comment>();
            for (Comment comment : comments) {
              if (comment.getBody().contains(PR_VALIDATOR)) {
                commentHash.put(comment.getId(), comment);
              }
            }

            if (commentHash.size() == 0) {
              logger.println("Initial Pull Request found, kicking off a build");
              isSupposedToRun = true;
              doRun(pullRequest, logger, issueService, commitService, repository, config);

            } else {
              Long mostRecentCommentId = Collections.max(commentHash.keySet());

              Comment mostRecentComment = commentHash.get(mostRecentCommentId);
              List<RepositoryCommit> commits = commitService.getCommits(repository, sha, null);
              if (mostRecentComment.getBody().contains(PR_VALIDATOR)) {
                for (RepositoryCommit commit : commits) {
                  if (commit.getCommit().getAuthor().getDate().after(mostRecentComment.getCreatedAt())) {
                    isSupposedToRun = true;
                  }
                }
              }
              if(!isSupposedToRun){
                logger.println("No new commits since the last build, not triggering build");
              }
              doRun(pullRequest, logger, issueService, commitService, repository, config);
            }

          }
        } catch (Exception ex) {
          LOGGER.info("Exception occurred stopping the trigger");
          LOGGER.info(ex.getMessage() + "\n" + ex.getStackTrace().toString());
        }

      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void doRun(final PullRequest pullRequest, PrintStream logger, IssueService issueService,
      CommitService commitService, Repository repository, PullRequestTriggerConfig localConfig) throws Exception {
    try {

      if (isSupposedToRun) {
        logger.println("Commit occurred after the last build, rebuilding");
        logger.println("Creating a comment and updating commit status");
        createCommentAndCommitStatus(issueService, commitService, repository, pullRequest);
        PullRequestTriggerConfig expandedConfig = localConfig;
        List<ParameterValue> stringParams = new ArrayList<ParameterValue>();
        stringParams.add(new StringParameterValue("systemUser", localConfig.getSystemUser()));
        stringParams.add(new PasswordParameterValue("systemUserPassword", localConfig.getSystemUserPassword()));
        stringParams.add(new StringParameterValue("repositoryName", localConfig.getRepositoryName()));
        stringParams.add(new StringParameterValue("repositoryOwner", localConfig.getRepositoryOwner()));
        stringParams.add(new StringParameterValue("gitHubRepository", localConfig.getGitHubRepository()));
        stringParams
            .add(new StringParameterValue("gitHubHeadRepository", pullRequest.getHead().getRepo().getCloneUrl()));
        stringParams.add(new StringParameterValue("sha", sha));
        stringParams.add(new StringParameterValue("pullRequestUrl", pullRequestUrl));
        stringParams.add(new StringParameterValue("pullRequestNumber", String.valueOf(pullRequest
            .getNumber())));
        ParametersAction params = new ParametersAction(stringParams);

        job.scheduleBuild2(0, new PullRequestTriggerCause(expandedConfig), params);

      }
    } catch (Exception ex) {
      LOGGER.info("Exception occurred stopping the trigger");
      LOGGER.info(ex.getMessage() + "\n" + ex.getStackTrace().toString());
    } finally {
      isSupposedToRun = false;
    }
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

  private String getPoolingComment() {
    StringBuilder sb = new StringBuilder();
    sb.append("<table cellspacing='0' cellpadding='0' ><tr><td align='left'><img src='");
    sb.append(Jenkins.getInstance().getRootUrl());
    sb.append("/favicon.ico' alt='" + PR_VALIDATOR + "'/></td>");
    sb.append("<td>");
    sb.append("PR Validator Started to Run Tests against your PR");
    sb.append("<br />");
    try {
      sb.append("<a target='_blank' href='" + this.job.getAbsoluteUrl()
          + "' title='Click here to view the Jenkins Job for the Fork that the pull request came from'>");
      sb.append("Click here to see Tests Running for " + job.getName());
      sb.append("</a>");
    } catch (IllegalStateException ise) {
      LOGGER.info("Exception occurred stopping the trigger");
      LOGGER.info(ise.getMessage() + "\n" + ise.getStackTrace().toString());
    }
    sb.append("</td>");
    sb.append("</tr></table>");
    return sb.toString();
  }

  @Override
  public Collection<? extends Action> getProjectActions() {
    if (job == null) {
      return Collections.emptyList();
    }

    return Collections.singleton(new PullRequestPollingAction());
  }

  /**
   * Action object for {@link Project}. Used to display the polling log.
   */
  public final class PullRequestPollingAction implements Action {
    public Job<?, ?> getOwner() {
      return job;
    }

    public String getIconFileName() {
      return "clipboard.png";
    }

    public String getDisplayName() {
      return "PR Validator Polling Log";
    }

    public String getUrlName() {
      return "PRValidatorPollLog";
    }

    public String getLog() throws IOException {
      return Util.loadFile(getLogFile());
    }

    public void writeLogTo(XMLOutput out) throws IOException {
      new AnnotatedLargeText<PullRequestPollingAction>(getLogFile(), Charsets.UTF_8, true, this)
          .writeHtmlTo(0, out.asWriter());
    }
  }

  public File getLogFile() throws IOException {
    File file = new File(job.getRootDir(), "PR-Validator.log");
    if (!file.exists()) {
      if (!file.getParentFile().exists()) {
        file.getParentFile().mkdirs();
      }
      file.createNewFile();
    }
    return file;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public static DescriptorImpl get() {
    return Trigger.all().get(DescriptorImpl.class);
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
