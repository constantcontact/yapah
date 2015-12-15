package PullRequestTriggerTest;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

/**
 *
 */
public class MockCommitService extends CommitService {
    public MockCommitService(GitHubClient gitHubClient) {

    }
}
