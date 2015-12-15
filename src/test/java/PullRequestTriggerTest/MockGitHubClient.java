package PullRequestTriggerTest;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.sisu.Nullable;

import java.util.ArrayList;

/**
 *
 */
public class MockGitHubClient extends GitHubClient {

    private ArrayList<String> credentials = new ArrayList<String>();

    @Override
    public MockGitHubClient setCredentials(String user, @Nullable String password) {
        getCredentials().add(user);
        getCredentials().add(password);
        return this;
    }

    public ArrayList<String> getCredentials() {
        return credentials;
    }

}
