package com.constantcontact.plugins.pullrequestvalidation;

import hudson.model.Cause;
import hudson.util.RobustReflectionConverter;

import org.kohsuke.stapler.export.Exported;

import com.google.common.base.Objects;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

public final class PullRequestTriggerCause extends Cause {

  private final String repositoryName;
  private final String credentialsId;
  private final String repositoryOwner;
  private final String gitHubRepository;

  public PullRequestTriggerCause(PullRequestTriggerConfig config) {
    this.repositoryName = config.getRepositoryName();
    this.credentialsId = config.getCredentialsId();
    this.repositoryOwner = config.getRepositoryOwner();
    this.gitHubRepository = config.getGitHubRepository();
  }
  
  @Override
  public String getShortDescription() {
    return "Pull request was created or modified";        
  }

  @Exported(visibility = 3)
  public String getRepositoryName() {
    return repositoryName;
  }

  @Exported(visibility = 3)
  public String getRepositoryOwner() {
    return repositoryOwner;
  }
  
  @Exported(visibility = 3)
  public String getCredentialsId() {
    return credentialsId;
  }

  @Exported(visibility = 3)
  public String getGitHubRepository() {
    return gitHubRepository;
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(repositoryName, credentialsId, repositoryOwner, gitHubRepository);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PullRequestTriggerCause) {
      PullRequestTriggerCause other = (PullRequestTriggerCause) obj;
      return Objects.equal(repositoryName, other.repositoryName)
          && credentialsId.equals(other.credentialsId) 
          && repositoryOwner.equals(other.repositoryOwner) && gitHubRepository.equals(other.gitHubRepository);
    }
    return false;
  }
  
  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("repositoryName", repositoryName)
        .add("credentialsId", credentialsId)
        .add("repositoryOwner", repositoryOwner).add("gitHubRepository", gitHubRepository).toString();
  }

  
  public static final class ConverterImpl extends RobustReflectionConverter {

    /**
     * Class constructor.
     * 
     * @param mapper
     *          the mapper
     */
    public ConverterImpl(Mapper mapper) {
      super(mapper, new PureJavaReflectionProvider());
    }
  }



}
