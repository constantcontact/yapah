package com.constantcontact.plugins.pullrequestvalidation;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class PullRequestCommenter extends Publisher implements SimpleBuildStep {

  @SuppressWarnings("deprecation")
  @DataBoundConstructor
  public PullRequestCommenter() {
    // TODO Auto-generated constructor stub
  }

  private static final String PR_VALIDATOR = Messages.getString("PullRequestCommenter.0"); //$NON-NLS-1$

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
      TaskListener listener) throws InterruptedException, IOException {

    final String sha = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.1")); //$NON-NLS-1$
    final String systemUser = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.2")); //$NON-NLS-1$
    final String systemUserPassword = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.3")); //$NON-NLS-1$
    final String repositoryName = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.4")); //$NON-NLS-1$
    final String repositoryOwner = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.5")); //$NON-NLS-1$
    final String pullRequestNumber = run.getEnvironment(listener).get(Messages.getString("PullRequestCommenter.6")); //$NON-NLS-1$

    GitHubClient githubClient = new GitHubClient(Messages.getString("PullRequestCommenter.7")); //$NON-NLS-1$
    githubClient.setCredentials(systemUser, systemUserPassword);

    RepositoryService repositoryService = new RepositoryService(githubClient);
    Repository repository = repositoryService.getRepository(repositoryOwner, repositoryName);

    CommitService commitService = new CommitService(githubClient);
    IssueService issueService = new IssueService(githubClient);
    StringBuilder sb = new StringBuilder();

    CommitStatus commitStatus = new CommitStatus();
    commitStatus.setState(CommitStatus.STATE_SUCCESS);

    if (run.getResult() == Result.SUCCESS) {
      commitStatus.setState(CommitStatus.STATE_SUCCESS);
      sb.append(Messages.getString("PullRequestCommenter.8")); //$NON-NLS-1$
    } else if (run.getResult() == Result.ABORTED) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.getString("PullRequestCommenter.9")); //$NON-NLS-1$
    } else if (run.getResult() == Result.FAILURE) {
      commitStatus.setState(CommitStatus.STATE_FAILURE);
      sb.append(Messages.getString("PullRequestCommenter.10")); //$NON-NLS-1$
    } else if (run.getResult() == Result.NOT_BUILT) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.getString("PullRequestCommenter.11")); //$NON-NLS-1$
    } else if (run.getResult() == Result.UNSTABLE) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.getString("PullRequestCommenter.12")); //$NON-NLS-1$
    }

    listener.getLogger().println(Messages.getString("PullRequestCommenter.13") + sb.toString().length()); //$NON-NLS-1$
    commitStatus.setDescription(sb.toString());
    commitService.createStatus(repository, sha, commitStatus);

    issueService.createComment(repository, pullRequestNumber,
        getPoolingComment(run));
  }

  private String getPoolingComment(Run<?, ?> run) {
    StringBuilder sb = new StringBuilder();
    sb.append(Messages.getString("PullRequestCommenter.14")); //$NON-NLS-1$
    sb.append(Jenkins.getInstance().getRootUrl());
    sb.append(Messages.getString("PullRequestCommenter.15") + PR_VALIDATOR + Messages.getString("PullRequestCommenter.16")); //$NON-NLS-1$ //$NON-NLS-2$
    sb.append(Messages.getString("PullRequestCommenter.17")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.18")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.19")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.20") + run.getAbsoluteUrl() //$NON-NLS-1$
        + Messages.getString("PullRequestCommenter.21")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.22") + run.getFullDisplayName()); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.23")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.24")); //$NON-NLS-1$
    sb.append(Messages.getString("PullRequestCommenter.25")); //$NON-NLS-1$
    return sb.toString();
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static final class DescriptorImpl extends
      BuildStepDescriptor<Publisher> {

    public DescriptorImpl() {
      load();
    }

    public FormValidation doValidation() {
      return FormValidation.ok();
    }

    @SuppressWarnings("rawtypes")
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    public String getDisplayName() {
      return Messages.getString("PullRequestCommenter.26"); //$NON-NLS-1$
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
        throws FormException {
      save();
      return super.configure(req, formData);
    }
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
  }

}
