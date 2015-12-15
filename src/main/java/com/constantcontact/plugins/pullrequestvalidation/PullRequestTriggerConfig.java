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

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
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

    public FormValidation doTestConnection(@QueryParameter("repositoryOwner") final String repositoryOwner,
        @QueryParameter("repositoryName") final String repositoryName,
        @QueryParameter("credentialsId") final String credentialsId,
        @QueryParameter("gitHubRepository") final String gitHubRepository) throws IOException, ServletException {

      StandardCredentials creds = CredentialHelper.lookupCredentials(credentialsId, gitHubRepository);
      if (null == creds) {
        return FormValidation.error(Messages.config_test_validation_nocreds());
      }
      StandardUsernamePasswordCredentials upCredentials = (StandardUsernamePasswordCredentials) creds;
      PullRequestTrigger.DescriptorImpl prDescriptor = (PullRequestTrigger.DescriptorImpl) Jenkins.getInstance()
          .getDescriptor(PullRequestTrigger.class);
      GitHubClient githubClient = new GitHubClient(prDescriptor.getGithubUrl());
      githubClient.setCredentials(upCredentials.getUsername(), upCredentials.getPassword().getPlainText());

      RepositoryService repositoryService = new RepositoryService(githubClient);
      try {
        repositoryService.getRepositories();
      } catch (IOException ioException) {
        return FormValidation
            .error(Messages.config_test_validation_nogh_1() + ioException.getLocalizedMessage());
      }
      Repository repository;
      try {
        repository = repositoryService.getRepository(repositoryOwner, repositoryName);
      } catch (IOException ioException) {
        return FormValidation.error(Messages.config_test_validation_norepo_1()
            + repositoryOwner + Messages.config_test_validation_norepo_2() + repositoryName + " "
            + ioException.getLocalizedMessage());
      }

      PullRequestService pullRequestService = new PullRequestService(githubClient);
      try {
        pullRequestService.getPullRequests(repository, "open");
      } catch (IOException ioException) {
        return FormValidation
            .error(Messages.config_test_validation_nopr()
                + ioException.getLocalizedMessage());
      }

      return FormValidation.ok("Success");
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
