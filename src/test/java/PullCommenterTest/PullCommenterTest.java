package PullCommenterTest;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Shell;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.constantcontact.plugins.pullrequestvalidation.PullRequestCommenter;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTrigger;
import com.constantcontact.plugins.pullrequestvalidation.PullRequestTriggerConfig;

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

}
