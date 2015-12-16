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

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

public class PullRequestCommenter extends Publisher implements SimpleBuildStep {

  @SuppressWarnings("deprecation")
  @DataBoundConstructor
  public PullRequestCommenter() {
    // TODO Auto-generated constructor stub
  }

  private static final String PR_VALIDATOR = "~PR_VALIDATOR_FINISH!~";
  
  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
      TaskListener listener) throws InterruptedException, IOException {

    final String sha = run.getEnvironment(listener).get("sha");
    final String credentialsId = run.getEnvironment(listener).get("credentialsId");
    final String repositoryName = run.getEnvironment(listener).get("repositoryName");
    final String repositoryOwner = run.getEnvironment(listener).get("repositoryOwner");
    final String pullRequestNumber = run.getEnvironment(listener).get("pullRequestNumber");
    final String localGithubUrl = run.getEnvironment(listener).get("localGithubUrl");
    final String bakedInTesting = run.getEnvironment(listener).get("bakedInTesting");

    HashMap<String,String> nullValidationForPostBuild = new HashMap<String,String>();
    nullValidationForPostBuild.put("sha",sha);
    nullValidationForPostBuild.put("credentialsId",credentialsId);
    nullValidationForPostBuild.put("repositoryName",repositoryName);
    nullValidationForPostBuild.put("repositoryOwner",repositoryOwner);
    nullValidationForPostBuild.put("pullRequestNumber",pullRequestNumber);
    nullValidationForPostBuild.put("localGithubUrl",localGithubUrl);
    nullValidationForPostBuild.put("bakedInTesting",bakedInTesting);

    String validationString = null;
    for(String validationKey: nullValidationForPostBuild.keySet()){
      validationString = nullValidationForPostBuild.get(validationKey);
      if(null == validationString || validationString == ""){
        listener.getLogger().println(validationKey.toString() + Messages.commenter_null_key_validation());
        listener.getLogger().println(Messages.commenter_null_validation());
        return;
      }
    }

    StandardCredentials credentials = CredentialHelper.lookupCredentials(null, credentialsId, localGithubUrl, listener.getLogger());
    StandardUsernamePasswordCredentials upCredentials = (StandardUsernamePasswordCredentials) credentials;
    
    GitHubClient githubClient = new GitHubClient(localGithubUrl);
    githubClient.setCredentials(upCredentials.getUsername(), upCredentials.getPassword().getPlainText());

    RepositoryService repositoryService = new RepositoryService(githubClient);
    Repository repository = repositoryService.getRepository(repositoryOwner, repositoryName);

    CommitService commitService = new CommitService(githubClient);
    IssueService issueService = new IssueService(githubClient);
    StringBuilder sb = new StringBuilder();

    CommitStatus commitStatus = new CommitStatus();
    commitStatus.setState(CommitStatus.STATE_SUCCESS);

    if (run.getResult() == Result.SUCCESS) {
      commitStatus.setState(CommitStatus.STATE_SUCCESS);
      sb.append(Messages.commenter_status_pass());
    } else if (run.getResult() == Result.ABORTED) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.commenter_status_aborted());
    } else if (run.getResult() == Result.FAILURE) {
      commitStatus.setState(CommitStatus.STATE_FAILURE);
      sb.append(Messages.commenter_status_failure());
    } else if (run.getResult() == Result.NOT_BUILT) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.commenter_status_notbuilt());
    } else if (run.getResult() == Result.UNSTABLE) {
      commitStatus.setState(CommitStatus.STATE_ERROR);
      sb.append(Messages.commenter_status_unstable());
    }

    commitStatus.setDescription(sb.toString());
    commitService.createStatus(repository, sha, commitStatus);

    issueService.createComment(repository, pullRequestNumber,
        getPoolingComment(run));
  }

  private String getPoolingComment(Run<?, ?> run) {
    StringBuilder sb = new StringBuilder();
    sb.append(Messages.commenter_pooling_comment_1());
    sb.append(Jenkins.getInstance().getRootUrl());
    sb.append(Messages.commenter_pooling_comment_2());
    sb.append(PR_VALIDATOR);
    sb.append(Messages.commenter_pooling_comment_3());
    sb.append(Messages.commenter_pooling_comment_4());
    sb.append(run.getAbsoluteUrl());
    sb.append(Messages.commenter_pooling_comment_5());
    sb.append(Messages.commenter_pooling_comment_6());
    sb.append(run.getFullDisplayName());
    sb.append(Messages.commenter_pooling_comment_7());
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
      return Messages.commenter_pooling_displayname();
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
