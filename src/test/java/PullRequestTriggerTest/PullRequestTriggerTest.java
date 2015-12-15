package PullRequestTriggerTest;

import com.constantcontact.plugins.pullrequestvalidation.GitHubBizLogic;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTrigger;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTriggerConfig;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PullRequestTriggerTest {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testAbilityToAddTrigger() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("BUILD-Test-Repo1");
    PullRequestTriggerConfig config = new PullRequestTriggerConfig("systemUser1", "systemPassword1", "repositoryName1", "repositoryOwner1", "gitHubRepository1", "sha1", "pullRequestUrl1");
    List<PullRequestTriggerConfig> configs = new ArrayList<PullRequestTriggerConfig>();
    configs.add(config);
    
    PullRequestTrigger trigger = new PullRequestTrigger("* * * * *", configs);
    project.addTrigger(trigger);
    project.getBuildersList().add(new Shell("echo hello"));
    project.save();  
    
    Map<TriggerDescriptor, Trigger<?>> testTriggers = project.getTriggers();
    for(Entry<?, ?> entry : testTriggers.entrySet()){
      PullRequestTrigger localTrigger = (PullRequestTrigger) entry.getValue();
      Assert.assertEquals(true, localTrigger.getSystemUser().contains("systemUser1"));
      Assert.assertEquals(true, localTrigger.getSystemUserPassword().contains(""));
      Assert.assertEquals(true, localTrigger.getRepositoryName().contains("repositoryName1"));
      Assert.assertEquals(true, localTrigger.getRepositoryOwner().contains("repositoryOwner1"));
      Assert.assertEquals(true, localTrigger.getGitHubRepository().contains("gitHubRepository1"));
    }

    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");
    String s = FileUtils.readFileToString(build.getLogFile());
    Assert.assertEquals(true, s.contains("+ echo hello"));
  }


  @Test
  public void testPreSetup() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    MockGitHubClient githubClient = new MockGitHubClient();
    MockRepositoryService repositoryService = new MockRepositoryService(githubClient);
    MockPullRequestService pullRequestService = new MockPullRequestService(githubClient);
    MockCommitService commitService = new MockCommitService(githubClient);
    MockIssueService issueService = new MockIssueService(githubClient);
    GitHubBizLogic gitHubWorker =
            new GitHubBizLogic(logger, githubClient, repositoryService, pullRequestService, commitService, issueService);

    String sysUser = "sysUser";
    String sysPassword = "sysPassword";
    String repoOwner = "repoOwner";
    String repoName = "repoName";
    String repo = "repo";
    String githubURL = "githubURL";

    //biz logic method under test
    List<PullRequest> pullRequests = gitHubWorker.doPreSetup(sysUser, sysPassword, repoOwner, repoName, repo, "githubURL");

    //test validations for pre-setup
    ArrayList<String> logMessages = logger.getLogEntries();
    Assert.assertEquals("Validate first log entry in pre setup", "Polling for " + repo, logMessages.get(0));
    Assert.assertEquals("Validate 2nd log entry in pre setup", "Instantiating Clients for " + githubURL, logMessages.get(1));

    ArrayList<String> credentials = githubClient.getCredentials();
    Assert.assertEquals("Validate setting gitHub user credentials", credentials.get(0), sysUser);
    Assert.assertEquals("Validate setting gitHub password credentials", credentials.get(1), sysPassword);

    Assert.assertNotNull("Validate repository was set", gitHubWorker.getRepository());
    ArrayList<String> repoInfo = repositoryService.getRepoInfo();
    Assert.assertEquals("Validate repo Owner input correctly", repoInfo.get(0), repoOwner);
    Assert.assertEquals("Validate repo Name input correctly", repoInfo.get(1), repoName);

    Assert.assertEquals("Validate 3rd log entry in pre setup", "Gathering Pull Requests for " + repo, logMessages.get(2));

    Assert.assertNotNull("Validate repository is set in pull request service", pullRequestService.getRepository());
    String requestedState = "open";
    Assert.assertEquals("Validate requested state of " + requestedState, requestedState, pullRequestService.getState());
    Assert.assertEquals("Validate returning pull requests", 2, pullRequests.get(0).getNumber());
  }
}
