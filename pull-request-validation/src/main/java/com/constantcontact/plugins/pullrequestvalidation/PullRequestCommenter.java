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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class PullRequestCommenter extends Publisher implements SimpleBuildStep {

  @SuppressWarnings("deprecation")
  @DataBoundConstructor
  public PullRequestCommenter() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
      TaskListener listener) throws InterruptedException, IOException {

    final String sha = run.getEnvironment(listener).get("sha");
    final String systemUser = run.getEnvironment(listener).get("systemUser");
    final String systemUserPassword = run.getEnvironment(listener).get("systemUserPassword");
    final String repositoryName = run.getEnvironment(listener).get("repositoryName");
    final String repositoryOwner = run.getEnvironment(listener).get("repositoryOwner");

    GitHubClient githubClient = new GitHubClient("github.roving.com");
    githubClient.setCredentials(systemUser, systemUserPassword);

    RepositoryService repositoryService = new RepositoryService(githubClient);
    Repository repository = repositoryService.getRepository(repositoryOwner, repositoryName);

    CommitService commitService = new CommitService(githubClient);

    StringBuilder sb = new StringBuilder();

    CommitStatus commitStatus = new CommitStatus();

    if (run.getResult() == Result.SUCCESS) {
      commitStatus.setState(CommitStatus.STATE_SUCCESS);
      sb.append("PR Validator: Good To Merge, all Tests Passed.");
    } else if (run.getResult() == Result.ABORTED) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append("PR Validator: NOT Good To Merge, job was Aborted.");
    } else if (run.getResult() == Result.FAILURE) {
      commitStatus.setState(CommitStatus.STATE_FAILURE);
      sb.append("PR Validator: NOT Good To Merge, Tests Failed!");
    } else if (run.getResult() == Result.NOT_BUILT) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append("PR Validator: NOT Good To Merge, job was not built.");
    } else if (run.getResult() == Result.UNSTABLE) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append("PR Validator: NOT Good To Merge, job was Unstable");
    }
       
    listener.getLogger().println("Description Length: " +  sb.toString().length());
    commitStatus.setDescription(sb.toString());
    commitService.createStatus(repository, sha, commitStatus);
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
      return "Github API Pull Request Validator";
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