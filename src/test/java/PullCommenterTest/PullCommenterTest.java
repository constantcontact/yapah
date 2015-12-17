package PullCommenterTest;

import PullRequestTriggerTest.MockCommitService;
import PullRequestTriggerTest.MockGitHubClient;
import PullRequestTriggerTest.MockIssueService;
import PullRequestTriggerTest.MockRepositoryService;
import com.constantcontact.plugins.pullrequestvalidation.GitHubBizLogic;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestCommenter;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTrigger;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTriggerConfig;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Shell;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.CommitStatus;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;

public class PullCommenterTest {
  
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testAbilityToAddPostBuildActionWithTrigger() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("BUILD-Test-Repo1");
    PullRequestTriggerConfig config = new PullRequestTriggerConfig("systemUser1", "repositoryName1", "repositoryOwner1", "gitHubRepository1", "sha1", "pullRequestUrl1");
    List<PullRequestTriggerConfig> configs = new ArrayList<PullRequestTriggerConfig>();
    configs.add(config);
    
    PullRequestTrigger trigger = new PullRequestTrigger("* * * * *", configs);
    project.addTrigger(trigger);
    project.getBuildersList().add(new Shell("echo hello"));
    PullRequestCommenter publisher = new PullRequestCommenter();
    project.addPublisher(publisher);
    project.save();  
            

    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");
    String s = FileUtils.readFileToString(build.getLogFile());
    System.out.println(s);
    Assert.assertEquals(true, s.contains("+ echo hello"));
  }
  
  @Test
  public void testAbilityToAddPostBuildActionWithOutTrigger() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("BUILD-Test-Repo2");
    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars envVars = prop.getEnvVars();
    envVars.put("bakedInTesting", "");
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);
    project.getBuildersList().add(new Shell("echo hello"));
    PullRequestCommenter publisher = new PullRequestCommenter();
    project.addPublisher(publisher);
    project.save();  
            

    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");
    String s = FileUtils.readFileToString(build.getLogFile());
    System.out.println(s);
    Assert.assertEquals(true, s.contains("+ echo hello"));
    Assert.assertEquals(true, s.contains("bakedInTesting was empty or null"));
    Assert.assertEquals(true, s.contains("Github Pull Request Poller needs to be setup as a trigger before being able to use this post build action."));
    
  }

  @Test
  public void testCreateCommitStatusAndComment() throws Exception {
    MockGitHubClient githubClient = new MockGitHubClient();
    MockRepositoryService repositoryService = new MockRepositoryService(githubClient);
    CommitStatus commitStatus = new CommitStatus();
    MockCommitService commitService = new MockCommitService(githubClient);
    //Comments used later in this test are defined in MockIssueService
    MockIssueService issueService = new MockIssueService(githubClient);
    GitHubBizLogic gitHubWorker = new GitHubBizLogic(githubClient, repositoryService, commitStatus, commitService, issueService);

    String systemUser = "sysUser";
    String systemPassword = "sysPassword";
    String repoOwner = "repoOwner";
    String repoName = "repoName";
    String state = "success";
    String desc = "desc";
    String sha = "sha";
    String pullRequestNumber = "2";
    String rootURL = "rootURL";
    String prValidator = "~PR_VALIDATOR_FINISH!~";
    String absoluteURL = "absoluteURL";
    String displayName = "displayName";
    //method under test
    gitHubWorker
            .createCommitStatusAndComment(systemUser, systemPassword, repoOwner, repoName, state, desc, sha, pullRequestNumber, rootURL, prValidator, absoluteURL, displayName);

    //test validations
    ArrayList<String> credentials = githubClient.getCredentials();
    Assert.assertEquals("Validate setting gitHub user credentials", credentials.get(0), systemUser);
    Assert.assertEquals("Validate setting gitHub password credentials", credentials.get(1), systemPassword);
    Assert.assertNotNull("Validate repository was set", gitHubWorker.getRepository());
    ArrayList<String> repoInfo = repositoryService.getRepoInfo();
    Assert.assertEquals("Validate repo Owner input correctly", repoInfo.get(0), repoOwner);
    Assert.assertEquals("Validate repo Name input correctly", repoInfo.get(1), repoName);
    Assert.assertEquals("Validate commit status state", state, commitStatus.getState());
    Assert.assertEquals("Validate commit status description", desc, commitStatus.getDescription());
    Assert.assertNotNull("Validate repository in commit service", commitService.getRepository());
    Assert.assertEquals("Validate sha setting in commit service", sha, commitService.getSha());
    Assert.assertEquals("Validate commit status state", state, commitService.getCommitStatus().getState());
    Assert.assertEquals("Validate commit status description", desc, commitService.getCommitStatus().getDescription());
    Assert.assertNotNull("Validate repository in issue service", issueService.getRepository());
    Assert.assertEquals("Validate pull request number in issue service", pullRequestNumber, issueService.getIssueNumber());
    String expectedComment = "<table cellspacing=0 cellpadding=0 ><tr><td align=left><img src=rootURL/favicon.ico " +
                             "alt=~PR_VALIDATOR_FINISH!~/></td><td>YAPah PR Validator Finished Running tests against your PR<br /><a target=_blank href=absoluteURL title=Click here to view the Jenkins Job for the Fork that the pull request came from>Click here to see Tests Running for displayName</a></td></tr></table>";
    Assert.assertEquals("Validate comment generated for issue service", expectedComment, issueService.getComment());
  }

}
