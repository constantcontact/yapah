package com.constantcontact.plugins.pullrequestvalidation;

import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.HashMap;

public class PullRequestCommenter extends Publisher implements SimpleBuildStep {

  private static final String PR_VALIDATOR = "~PR_VALIDATOR_FINISH!~";

  @SuppressWarnings("deprecation")
  @DataBoundConstructor
  public PullRequestCommenter() {
    // TODO Auto-generated constructor stub
  }
  
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
    boolean doCommentWork = true;
    for(String validationKey: nullValidationForPostBuild.keySet()){
      validationString = nullValidationForPostBuild.get(validationKey);
      if(null == validationString || validationString == ""){
        doCommentWork = false;
        listener.getLogger().println(validationKey.toString() + " " + Messages.commenter_null_key_validation());
      }
    }
    
    if(!doCommentWork){
      listener.getLogger().println(Messages.commenter_null_validation());
      return;
    }

    StandardCredentials credentials = CredentialHelper.lookupCredentials(null, credentialsId, localGithubUrl, listener.getLogger());
    StandardUsernamePasswordCredentials upCredentials = (StandardUsernamePasswordCredentials) credentials;

    String state = CommitStatus.STATE_SUCCESS;
    String desc = "";

    if (run.getResult() == Result.SUCCESS) {
      state = CommitStatus.STATE_SUCCESS;
      desc = Messages.commenter_status_pass();
    } else if (run.getResult() == Result.ABORTED) {
      state = CommitStatus.STATE_ERROR;
      desc = Messages.commenter_status_aborted();
    } else if (run.getResult() == Result.FAILURE) {
      state = CommitStatus.STATE_FAILURE;
      desc = Messages.commenter_status_failure();
    } else if (run.getResult() == Result.NOT_BUILT) {
      state = CommitStatus.STATE_ERROR;
      desc = Messages.commenter_status_notbuilt();
    } else if (run.getResult() == Result.UNSTABLE) {
      state = CommitStatus.STATE_ERROR;
      desc = Messages.commenter_status_unstable();
    }

    GitHubClient githubClient = new GitHubClient(localGithubUrl);
    RepositoryService repositoryService = new RepositoryService(githubClient);
    CommitStatus commitStatus = new CommitStatus();
    CommitService commitService = new CommitService(githubClient);
    IssueService issueService = new IssueService(githubClient);
    GitHubBizLogic gitHubWorker = new GitHubBizLogic(githubClient, repositoryService, commitStatus, commitService, issueService);


    gitHubWorker.createCommitStatusAndComment(upCredentials.getUsername(), upCredentials.getPassword()
                                                                                        .getPlainText(), repositoryOwner, repositoryName, state, desc, sha, pullRequestNumber, Jenkins
                                                      .getInstance().getRootUrl(), PR_VALIDATOR, run.getAbsoluteUrl(), run
                                                      .getFullDisplayName());
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.BUILD;
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

}
