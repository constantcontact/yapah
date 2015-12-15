package com.constantcontact.plugins.pullrequestvalidation;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Item;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.ACL;
import hudson.slaves.NodeProperty;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.FormValidation;
import hudson.util.RobustReflectionConverter;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.base.Objects;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

public final class PullRequestTriggerConfig implements Describable<PullRequestTriggerConfig> {

  private String repositoryName;
  private String credentialsId;
  private String repositoryOwner;
  private String gitHubRepository;
  private String sha;
  private String pullRequestUrl;

  public static DescriptorImpl getClassDescriptor() {
    return (DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(
        PullRequestTriggerConfig.class);
  }

  @DataBoundConstructor
  public PullRequestTriggerConfig(String credentialsId, String repositoryName,
      String repositoryOwner, String gitHubRepository, String sha, String pullRequestUrl) {
    this.repositoryName = repositoryName;
    this.repositoryOwner = repositoryOwner;
    this.credentialsId = credentialsId;
    this.gitHubRepository = gitHubRepository;
    this.sha = sha;
    this.pullRequestUrl = pullRequestUrl;
  }

  @SuppressWarnings("unused")
  // called reflectively by XStream
  private PullRequestTriggerConfig() {
    this.repositoryName = null;
    this.repositoryOwner = "";
    this.credentialsId = "";
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
    String expCredentialsId = credentialsId == null ? null : vars.expand(credentialsId);
    String expGitHubRepository = gitHubRepository == null ? null : vars.expand(gitHubRepository);
    String expSha = sha == null ? null : vars.expand(sha);
    String expPullRequestUrl = pullRequestUrl == null ? null : vars.expand(pullRequestUrl);

    return new PullRequestTriggerConfig(expCredentialsId, expRepositoryName, expRepositoryOwner,
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

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String serverAPIUrl,
        @QueryParameter String credentialsId) throws URISyntaxException {
      List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(serverAPIUrl).build();

      List<CredentialsMatcher> matchers = new ArrayList<CredentialsMatcher>(3);
      if (!StringUtils.isEmpty(credentialsId)) {
        matchers.add(0, CredentialsMatchers.withId(credentialsId));
      }

      matchers.add(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
      matchers.add(CredentialsMatchers.instanceOf(StringCredentials.class));

      List<StandardCredentials> credentials = CredentialsProvider.lookupCredentials(
          StandardCredentials.class,
          context,
          ACL.SYSTEM,
          domainRequirements
          );

      return new StandardListBoxModel()
          .withMatching(
              CredentialsMatchers.anyOf(
                  matchers.toArray(new CredentialsMatcher[0])),
              credentials
          );
    }

    public FormValidation doTestConfiguration(
        @QueryParameter("repositoryName") final String repositoryName,
        @QueryParameter("systemUser") final String systemUser,
        @QueryParameter("systemUserPassword") final String systemUserPassword,
        @QueryParameter("repositoryOwner") final String repositoryOwner,
        @QueryParameter("gitHubRepository") final String gitHubRepository)
        throws IOException, InterruptedException {

      if (repositoryName.length() < 1) {
        return FormValidation.error(Messages.config_form_validation_1());
      }

      if (systemUser.length() < 1) {
        return FormValidation.error(Messages.config_form_validation_2());
      }

      if (systemUserPassword.length() < 1) {
        return FormValidation.error(Messages.config_form_validation_3());
      }

      if (repositoryOwner.length() < 1) {
        return FormValidation.error(Messages.config_form_validation_4());
      }

      if (gitHubRepository.length() < 1) {
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
        .add("credentialsId", credentialsId)
        .add("repositoryOwner", repositoryOwner).add("gitHubRepository", gitHubRepository).toString();
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public void setCredentialsId(String credentialsId) {
    this.credentialsId = credentialsId;
  }

}
