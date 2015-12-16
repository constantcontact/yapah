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
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.*;
import java.util.Map.Entry;

public class PullRequestTriggerTest {

  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  @Test
  public void testAbilityToAddTrigger() throws Exception {
    FreeStyleProject project = jenkinsRule.createFreeStyleProject("BUILD-Test-Repo1");
    PullRequestTriggerConfig config = new PullRequestTriggerConfig("systemUser1", "repositoryName1", "repositoryOwner1", "gitHubRepository1", "sha1", "pullRequestUrl1");
    List<PullRequestTriggerConfig> configs = new ArrayList<PullRequestTriggerConfig>();
    configs.add(config);

    PullRequestTrigger trigger = new PullRequestTrigger("* * * * *", configs);
    project.addTrigger(trigger);
    project.getBuildersList().add(new Shell("echo hello"));
    project.save();

    Map<TriggerDescriptor, Trigger<?>> testTriggers = project.getTriggers();
    for (Entry<?, ?> entry : testTriggers.entrySet()) {
      PullRequestTrigger localTrigger = (PullRequestTrigger) entry.getValue();
      Assert.assertEquals(true, localTrigger.getCredentialsId().contains("systemUser1"));
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
    List<PullRequest> pullRequests = gitHubWorker.doPreSetup(sysUser, sysPassword, repoOwner, repoName, repo, githubURL);

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

  private GitHubBizLogic initialize(MockLogWriter logger) {

    MockGitHubClient githubClient = new MockGitHubClient();
    MockRepositoryService repositoryService = new MockRepositoryService(githubClient);
    MockPullRequestService pullRequestService = new MockPullRequestService(githubClient);
    MockCommitService commitService = new MockCommitService(githubClient);
    MockIssueService issueService = new MockIssueService(githubClient);
    return new GitHubBizLogic(logger, githubClient, repositoryService, pullRequestService, commitService, issueService);
  }

  @Test
  public void testLogZeroPR() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    GitHubBizLogic gitHubWorker = initialize(logger);

    ArrayList<String> logMessages = logger.getLogEntries();
    String repo = "repo";
    //method to test
    gitHubWorker.logZeroPR(repo);
    Assert.assertEquals("Validate zero pr log entry", "Found no Pull Requests for " + repo, logMessages.get(0));
  }

  @Test
  public void testLogSHA() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    GitHubBizLogic gitHubWorker = initialize(logger);

    ArrayList<String> logMessages = logger.getLogEntries();
    String sha = "sha";
    //method to test
    gitHubWorker.logSHA(sha);
    Assert.assertEquals("Validate sha log entry", "Got SHA1 : " + sha, logMessages.get(0));
  }

  @Test
  public void testPRURL() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    GitHubBizLogic gitHubWorker = initialize(logger);

    ArrayList<String> logMessages = logger.getLogEntries();
    String url = "url";
    //method to test
    gitHubWorker.logPRURL(url);
    Assert.assertEquals("Validate sha log entry", "Pull Request URL : " + url, logMessages.get(0));
  }

  @Test
  public void testCaptureComments() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    MockGitHubClient githubClient = new MockGitHubClient();
    MockRepositoryService repositoryService = new MockRepositoryService(githubClient);
    MockPullRequestService pullRequestService = new MockPullRequestService(githubClient);
    MockCommitService commitService = new MockCommitService(githubClient);
    //Comments used later in this test are defined in MockIssueService
    MockIssueService issueService = new MockIssueService(githubClient);
    GitHubBizLogic gitHubWorker =
            new GitHubBizLogic(logger, githubClient, repositoryService, pullRequestService, commitService, issueService);

    String sysUser = "sysUser";
    String sysPassword = "sysPassword";
    String repoOwner = "repoOwner";
    String repoName = "repoName";
    String repo = "repo";
    String githubURL = "githubURL";

    List<PullRequest> pullRequests = gitHubWorker.doPreSetup(sysUser, sysPassword, repoOwner, repoName, repo, githubURL);
    String commentBodyIndicator = "~PR_VALIDATOR";
    //method to test
    HashMap<Long, Comment> comments = gitHubWorker.captureComments(repoOwner, repoName, pullRequests.get(0), commentBodyIndicator);

    Comment firstComment = comments.get(2L);
    Comment secondComment = comments.get(3L);

    Assert.assertEquals("Validate correct number of comments returned", 2, comments.size());
    Assert.assertEquals("Validate first comment id", 2L, firstComment.getId());
    Assert.assertEquals("Validate second comment id", 3L, secondComment.getId());
    Assert.assertTrue("Validate first comment body", firstComment.getBody().contains(commentBodyIndicator));
    Assert.assertTrue("Validate second comment body", secondComment.getBody().contains(commentBodyIndicator));
  }

  @Test
  public void testDoZeroCommentsWork() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    GitHubBizLogic gitHubWorker = initialize(logger);

    ArrayList<String> logMessages = logger.getLogEntries();
    //method to test
    boolean shouldRun = gitHubWorker.doZeroCommentsWork(false);
    Assert.assertEquals("Validate zero comments work log entry", "Initial Pull Request found, kicking off a build", logMessages.get(0));
    Assert.assertTrue("Validate should run setting when initially set to false", shouldRun);

    boolean shouldRun2 = gitHubWorker.doZeroCommentsWork(true);
    Assert.assertTrue("Validate should run setting when initially set to true", shouldRun2);
  }

  @Test
  public void testDoNonZeroCommentsWork() throws Exception {
    //test setup
    MockLogWriter logger = new MockLogWriter();
    MockGitHubClient githubClient = new MockGitHubClient();
    MockRepositoryService repositoryService = new MockRepositoryService(githubClient);
    MockPullRequestService pullRequestService = new MockPullRequestService(githubClient);
    //Commits used later in this test are defined in MockCommitService
    MockCommitService commitService = new MockCommitService(githubClient);
    //Comments used later in this test are defined in MockIssueService
    MockIssueService issueService = new MockIssueService(githubClient);
    GitHubBizLogic gitHubWorker =
            new GitHubBizLogic(logger, githubClient, repositoryService, pullRequestService, commitService, issueService);

    String sysUser = "sysUser";
    String sysPassword = "sysPassword";
    String repoOwner = "repoOwner";
    String repoName = "repoName";
    String repo = "repo";
    String githubURL = "githubURL";

    List<PullRequest> pullRequests = gitHubWorker.doPreSetup(sysUser, sysPassword, repoOwner, repoName, repo, githubURL);
    String commentBodyIndicator = "~PR_VALIDATOR";
    HashMap<Long, Comment> commentHash = gitHubWorker.captureComments(repoOwner, repoName, pullRequests.get(0), commentBodyIndicator);

    //method to test
    boolean shouldRun = gitHubWorker.doNonZeroCommentsWork(false, commentHash, "sha", commentBodyIndicator);

    Assert.assertFalse("Validate shouldrun setting", shouldRun);
    ArrayList<String> logMessages = logger.getLogEntries();
    Assert.assertEquals("Validate zero comments work log entry", "No new commits since the last build, not triggering build", logMessages
            .get(logMessages.size() - 1));
    boolean shouldRun2 = gitHubWorker.doNonZeroCommentsWork(true, commentHash, "sha", commentBodyIndicator);

    Assert.assertTrue("Validate shouldrun setting", shouldRun2);
    ArrayList<String> logMessages2 = logger.getLogEntries();
    Assert.assertEquals("Validate zero comments work log entry", "No new commits since the last build, not triggering build", logMessages2
            .get(logMessages2.size() - 1));


    commitService.setCommitDate(new GregorianCalendar(2016, Calendar.FEBRUARY, 11).getTime());
    //method to test
    boolean shouldRun3 = gitHubWorker.doNonZeroCommentsWork(false, commentHash, "sha", commentBodyIndicator);
    Assert.assertTrue("Validate shouldrun setting", shouldRun3);
  }
}
