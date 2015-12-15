package PullRequestTriggerTest;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.constantcontact.plugins.pullrequestvalidation.PullRequestTrigger;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTriggerConfig;

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
}
