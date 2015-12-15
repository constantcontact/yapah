package com.constantcontact.plugins.pullrequestvalidation;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.NodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.FormValidation;
import hudson.util.RobustReflectionConverter;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Objects;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

public final class PullRequestTriggerConfig implements Describable<PullRequestTriggerConfig> {

  private String repositoryName;
  private String systemUser;
  private String systemUserPassword;
  private String repositoryOwner;
  private String gitHubRepository;
  private String sha;
  private String pullRequestUrl;

  public static DescriptorImpl getClassDescriptor() {
    return (DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(
        PullRequestTriggerConfig.class);
  }

  @DataBoundConstructor
  public PullRequestTriggerConfig(String systemUser, String systemUserPassword, String repositoryName,
      String repositoryOwner, String gitHubRepository, String sha, String pullRequestUrl) {
    this.repositoryName = repositoryName;
    this.repositoryOwner = repositoryOwner;
    this.systemUser = systemUser;
    this.systemUserPassword = systemUserPassword;
    this.gitHubRepository = gitHubRepository;
    this.sha = sha;
    this.pullRequestUrl = pullRequestUrl;
  }
  
  @SuppressWarnings("unused")
  // called reflectively by XStream
  private PullRequestTriggerConfig() {
    this.repositoryName = null;
    this.repositoryOwner = "";
    this.systemUser = "";
    this.systemUserPassword = "";
    this.gitHubRepository = "";
    this.sha = "";
    this.pullRequestUrl = "";

  }

  @Override
  public Descriptor<PullRequestTriggerConfig> getDescriptor() {
    return getClassDescriptor();
  }

  PullRequestTriggerConfig expand() {
    EnvVars vars = new EnvVars();

    // Environment variables
    vars.overrideAll(System.getenv());

    // Global properties
    for (NodeProperty<?> property : Hudson.getInstance()
        .getGlobalNodeProperties()) {
      if (property instanceof EnvironmentVariablesNodeProperty) {
        vars.overrideAll(((EnvironmentVariablesNodeProperty) property)
            .getEnvVars());
      }
    }

    // Expand each field
    String expRepositoryName = repositoryName == null ? null : vars.expand(repositoryName);
    String expRepositoryOwner = repositoryOwner == null ? null : vars.expand(repositoryOwner);
    String expSystemUser = systemUser == null ? null : vars.expand(systemUser);
    String expSystemUserPassword = systemUserPassword == null ? null : vars.expand(systemUserPassword);
    String expGitHubRepository = gitHubRepository == null ? null : vars.expand(gitHubRepository);
    String expSha = sha == null ? null : vars.expand(sha);
    String expPullRequestUrl = pullRequestUrl == null ? null : vars.expand(pullRequestUrl);

    return new PullRequestTriggerConfig(expSystemUser, expSystemUserPassword, expRepositoryName, expRepositoryOwner,
        expGitHubRepository, expSha, expPullRequestUrl);
  }

  public static final class ConverterImpl extends RobustReflectionConverter {

    public ConverterImpl(Mapper mapper) {
      super(mapper, new PureJavaReflectionProvider());
    }
  }

  @Extension
  public static final class DescriptorImpl extends
      Descriptor<PullRequestTriggerConfig> {

    @Override
    public String getDisplayName() {
      // Not used.
      return "";
    }

    public FormValidation doTestConfiguration(
        @QueryParameter("repositoryName") final String repositoryName,
        @QueryParameter("systemUser") final String systemUser,
        @QueryParameter("systemUserPassword") final String systemUserPassword,
        @QueryParameter("repositoryOwner") final String repositoryOwner,
        @QueryParameter("gitHubRepository") final String gitHubRepository)
        throws IOException, InterruptedException {
      
      if(repositoryName.length() < 1){
        return FormValidation.error(Messages.config_form_validation_1());
      }
      
      if(systemUser.length() < 1){
        return FormValidation.error(Messages.config_form_validation_2());
      }
      
      if(systemUserPassword.length() < 1){
        return FormValidation.error(Messages.config_form_validation_3());
      }
      
      if(repositoryOwner.length() < 1){
        return FormValidation.error(Messages.config_form_validation_4());
      }
      
      if(gitHubRepository.length() < 1){
        return FormValidation.error(Messages.config_form_validation_5());
      }

      return FormValidation.ok();
    }
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getSystemUser() {
    return systemUser;
  }

  public void setSystemUser(String systemUser) {
    this.systemUser = systemUser;
  }

  public String getSystemUserPassword() {
    return systemUserPassword;
  }

  public void setSystemUserPassword(String systemUserPassword) {
    this.systemUserPassword = systemUserPassword;
  }

  public String getRepositoryOwner() {
    return repositoryOwner;
  }

  public void setRepositoryOwner(String repositoryOwner) {
    this.repositoryOwner = repositoryOwner;
  }

  public String getGitHubRepository() {
    return gitHubRepository;
  }

  public void setGitHubRepository(String gitHubRepository) {
    this.gitHubRepository = gitHubRepository;
  }
  
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("repositoryName", repositoryName)
        .add("systemUser", systemUser).add("systemUserPassword", systemUserPassword)
        .add("repositoryOwner", repositoryOwner).add("gitHubRepository", gitHubRepository).toString();
  }

}
