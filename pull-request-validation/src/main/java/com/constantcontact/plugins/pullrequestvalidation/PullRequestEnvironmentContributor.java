package com.constantcontact.plugins.pullrequestvalidation;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;

public final class PullRequestEnvironmentContributor extends EnvironmentContributor {

  @Override
  public void buildEnvironmentFor(@SuppressWarnings("rawtypes") Run r,
      EnvVars envs, TaskListener listener) {
    buildEnvironmentFor(r, envs);
  }

  private void buildEnvironmentFor(Run<?, ?> run, EnvVars envVars) {
    PullRequestTriggerCause cause = run.getCause(PullRequestTriggerCause.class);
    if (cause != null) {
      envVars.put(name(Messages.getString("PullRequestEnvironmentContributor.0")), cause.getRepositoryName()); //$NON-NLS-1$
      envVars.put(name(Messages.getString("PullRequestEnvironmentContributor.1")), cause.getRepositoryOwner()); //$NON-NLS-1$
      envVars.put(name(Messages.getString("PullRequestEnvironmentContributor.2")), cause.getSystemUser()); //$NON-NLS-1$
      envVars.put(name(Messages.getString("PullRequestEnvironmentContributor.3")), cause.getSystemUserPassword()); //$NON-NLS-1$
      envVars.put(name(Messages.getString("PullRequestEnvironmentContributor.4")), cause.getGitHubRepository()); //$NON-NLS-1$
    }
  }

  private String name(String envVar) {
    return Messages.getString("PullRequestEnvironmentContributor.5") + envVar; //$NON-NLS-1$
  }

}
