package com.constantcontact.plugins.pullrequestvalidation;

import hudson.model.Item;
import hudson.security.ACL;

import java.io.PrintStream;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

public class CredentialHelper {

  public static StandardCredentials lookupCredentials(Item context, String credentialId, String uri,
      PrintStream printStream) {
    String contextName = "(Jenkins.instance)";
    if (context != null) {
      contextName = context.getFullName();
    }
    printStream.println(Messages.credshelper_log_1() + credentialId + Messages.credshelper_log_2() + contextName
        + Messages.credshelper_log_3() + uri);

    List<StandardCredentials> credentials;

    printStream.println(Messages.credshelper_log_4());

    credentials = CredentialsProvider.lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM,
        URIRequirementBuilder.fromUri(uri).build());

    printStream.println(Messages.credshelper_log_5() + credentials.size() + Messages.credshelper_log_6());

    return (credentialId == null) ? null : CredentialsMatchers.firstOrNull(credentials,
        CredentialsMatchers.withId(credentialId));
  }

}
