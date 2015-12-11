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
  private static final String                       PR_VALIDATOR    = Messages.getString("PullRequestTrigger.0"); //$NON-NLS-1$

  private final ArrayList<PullRequestTriggerConfig> additionalConfigs;

  @DataBoundConstructor
  public PullRequestTrigger(String spec, List<PullRequestTriggerConfig> configs) throws ANTLRException {
    super(spec);

    ArrayList<PullRequestTriggerConfig> configsCopy = new ArrayList<PullRequestTriggerConfig>(
        fixNull(configs));

    PullRequestTriggerConfig firstConfig;

    if (configsCopy.isEmpty()) {
      firstConfig = new PullRequestTriggerConfig(Messages.getString("PullRequestTrigger.1"), null, null, null, null, null, null); //$NON-NLS-1$
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
    this.repositoryOwner = Messages.getString("PullRequestTrigger.2"); //$NON-NLS-1$
    this.systemUser = Messages.getString("PullRequestTrigger.3"); //$NON-NLS-1$
    this.systemUserPassword = Messages.getString("PullRequestTrigger.4"); //$NON-NLS-1$
    this.gitHubRepository = Messages.getString("PullRequestTrigger.5"); //$NON-NLS-1$
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
    sb.append(Messages.getString("PullRequestTrigger.6")); //$NON-NLS-1$
    sb.append(project.getName());
    return sb.toString();
  }

  @Override
  public void run() {
    for (PullRequestTriggerConfig config : getConfigs()) {
      if (null == config.getGitHubRepository()) {
        return;
      }
      StreamTaskListener listener = null;
      try {
        this.sha = null;
        this.pullRequestUrl = null;

        listener = new StreamTaskListener(getLogFile());
        PrintStream logger = listener.getLogger();

        logger.println(Messages.getString("PullRequestTrigger.7") + config.getGitHubRepository()); //$NON-NLS-1$

        logger.println(Messages.getString("PullRequestTrigger.8")); //$NON-NLS-1$
        GitHubClient githubClient = new GitHubClient(Messages.getString("PullRequestTrigger.9")); //$NON-NLS-1$
        githubClient.setCredentials(config.getSystemUser(), config.getSystemUserPassword());

        RepositoryService repositoryService = new RepositoryService(githubClient);
        Repository repository = repositoryService
            .getRepository(config.getRepositoryOwner(), config.getRepositoryName());

        PullRequestService pullRequestService = new PullRequestService(githubClient);
        logger.println(Messages.getString("PullRequestTrigger.10") + config.getGitHubRepository()); //$NON-NLS-1$

        List<PullRequest> pullRequests = pullRequestService.getPullRequests(repository, Messages.getString("PullRequestTrigger.11")); //$NON-NLS-1$
        CommitService commitService = new CommitService(githubClient);
        IssueService issueService = new IssueService(githubClient);

        if (pullRequests.size() == 0) {
          logger.println(Messages.getString("PullRequestTrigger.12") + config.getGitHubRepository()); //$NON-NLS-1$
          continue;
        }

        for (PullRequest pullRequest : pullRequests) {
          this.sha = pullRequest.getHead().getSha();
          logger.println(Messages.getString("PullRequestTrigger.13") + this.sha); //$NON-NLS-1$

          this.pullRequestUrl = pullRequest.getUrl();
          logger.println(Messages.getString("PullRequestTrigger.14") + this.pullRequestUrl); //$NON-NLS-1$

          List<Comment> comments = issueService.getComments(config.getRepositoryOwner(), config.getRepositoryName(),
              pullRequest.getNumber());

          HashMap<Long, Comment> commentHash = new HashMap<Long, Comment>();
          for (Comment comment : comments) {
            LOGGER.info(Messages.getString("PullRequestTrigger.15") + comment.getBody()); //$NON-NLS-1$
            if (comment.getBody().contains(PR_VALIDATOR)) {
              commentHash.put(comment.getId(), comment);
            }
          }

          if (commentHash.size() == 0) {
            logger.println(Messages.getString("PullRequestTrigger.16")); //$NON-NLS-1$
            LOGGER.info(Messages.getString("PullRequestTrigger.17")); //$NON-NLS-1$
            isSupposedToRun = true;
            doRun(pullRequest, logger, issueService, commitService, repository, config);

          } else {
            Long mostRecentCommentId = Collections.max(commentHash.keySet());
            
            Comment mostRecentComment = commentHash.get(mostRecentCommentId);
            LOGGER.info(Messages.getString("PullRequestTrigger.18") + mostRecentCommentId); //$NON-NLS-1$

            List<RepositoryCommit> commits = commitService.getCommits(repository, sha, null);
            if (mostRecentComment.getBody().contains(PR_VALIDATOR)) {
              for (RepositoryCommit commit : commits) {
                if (commit.getCommit().getAuthor().getDate().after(mostRecentComment.getCreatedAt())) {
                  isSupposedToRun = true;
                }
              }
            }

            doRun(pullRequest, logger, issueService, commitService, repository, config);
          }

        }
      } catch (Exception ex) {
        LOGGER.info(Messages.getString("PullRequestTrigger.19")); //$NON-NLS-1$
        LOGGER.info(ex.getMessage() + Messages.getString("PullRequestTrigger.20") + ex.getStackTrace().toString()); //$NON-NLS-1$
      }

    }
  }

  private void doRun(final PullRequest pullRequest, PrintStream logger, IssueService issueService,
      CommitService commitService, Repository repository, PullRequestTriggerConfig localConfig) throws Exception {
    try {

      if (isSupposedToRun) {
        logger.println(Messages.getString("PullRequestTrigger.21")); //$NON-NLS-1$
        logger.println(Messages.getString("PullRequestTrigger.22")); //$NON-NLS-1$
        createCommentAndCommitStatus(issueService, commitService, repository, pullRequest);
        PullRequestTriggerConfig expandedConfig = localConfig;
        List<ParameterValue> stringParams = new ArrayList<ParameterValue>();
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.23"), localConfig.getSystemUser())); //$NON-NLS-1$
        stringParams.add(new PasswordParameterValue(Messages.getString("PullRequestTrigger.24"), localConfig.getSystemUserPassword())); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.25"), localConfig.getRepositoryName())); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.26"), localConfig.getRepositoryOwner())); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.27"), localConfig.getGitHubRepository())); //$NON-NLS-1$
        stringParams
            .add(new StringParameterValue(Messages.getString("PullRequestTrigger.28"), pullRequest.getHead().getRepo().getCloneUrl())); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.29"), sha)); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.30"), pullRequestUrl)); //$NON-NLS-1$
        stringParams.add(new StringParameterValue(Messages.getString("PullRequestTrigger.31"), String.valueOf(pullRequest //$NON-NLS-1$
            .getNumber())));
        ParametersAction params = new ParametersAction(stringParams);

        job.scheduleBuild2(0, new PullRequestTriggerCause(expandedConfig), params);

      }
    } catch (Exception ex) {
      LOGGER.info(Messages.getString("PullRequestTrigger.32")); //$NON-NLS-1$
      LOGGER.info(ex.getMessage() + Messages.getString("PullRequestTrigger.33") + ex.getStackTrace().toString()); //$NON-NLS-1$
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
    sb.append(Messages.getString("PullRequestTrigger.34")); //$NON-NLS-1$
    sb.append(Jenkins.getInstance().getRootUrl());
    sb.append(Messages.getString("PullRequestTrigger.35")+PR_VALIDATOR+Messages.getString("PullRequestTrigger.36")); //$NON-NLS-1$ //$NON-NLS-2$
    sb.append(Messages.getString("PullRequestTrigger.37")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestTrigger.38")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestTrigger.39")); //$NON-NLS-1$
    try {
      sb.append(Messages.getString("PullRequestTrigger.40") + this.job.getAbsoluteUrl() //$NON-NLS-1$
          + Messages.getString("PullRequestTrigger.41")); //$NON-NLS-1$
      sb.append(Messages.getString("PullRequestTrigger.42") + job.getName()); //$NON-NLS-1$
      sb.append(Messages.getString("PullRequestTrigger.43")); //$NON-NLS-1$
    } catch (IllegalStateException ise) {
      LOGGER.info(Messages.getString("PullRequestTrigger.44")); //$NON-NLS-1$
      LOGGER.info(ise.getMessage() + Messages.getString("PullRequestTrigger.45") + ise.getStackTrace().toString()); //$NON-NLS-1$
    }
    sb.append(Messages.getString("PullRequestTrigger.46")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestTrigger.47")); //$NON-NLS-1$
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
      return Messages.getString("PullRequestTrigger.48"); //$NON-NLS-1$
    }

    public String getDisplayName() {
      return Messages.getString("PullRequestTrigger.49"); //$NON-NLS-1$
    }

    public String getUrlName() {
      return Messages.getString("PullRequestTrigger.50"); //$NON-NLS-1$
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
    File file = new File(job.getRootDir(), Messages.getString("PullRequestTrigger.51")); //$NON-NLS-1$
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
      return Messages.getString("PullRequestTrigger.52"); //$NON-NLS-1$
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
