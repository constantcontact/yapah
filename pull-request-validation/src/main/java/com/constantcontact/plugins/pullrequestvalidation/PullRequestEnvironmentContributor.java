//package com.constantcontact.plugins.pullrequestvalidation;
//
//import hudson.EnvVars;
//import hudson.model.TaskListener;
//import hudson.model.EnvironmentContributor;
//import hudson.model.Run;
//
//public final class PullRequestEnvironmentContributor extends EnvironmentContributor {
//
//  @Override
//  public void buildEnvironmentFor(@SuppressWarnings("rawtypes") Run r,
//      EnvVars envs, TaskListener listener) {
//    buildEnvironmentFor(r, envs);
//  }
//
//  private void buildEnvironmentFor(Run<?, ?> run, EnvVars envVars) {
//    PullRequestTriggerCause cause = run.getCause(PullRequestTriggerCause.class);
//    if (cause != null) {
//      envVars.put(name("repositoryName"), cause.getRepositoryName());
//      envVars.put(name("repositoryOwner"), cause.getRepositoryOwner());
//      envVars.put(name("systemUser"), cause.getSystemUser());
//      envVars.put(name("systemUserPassword"), cause.getSystemUserPassword());
//      envVars.put(name("gitHubRepository"), cause.getGitHubRepository());
//    }
//  }
//
//  private String name(String envVar) {
//    return "var_setting_" + envVar;
//  }
//
//}
