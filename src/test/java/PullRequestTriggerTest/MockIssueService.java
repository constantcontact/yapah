package PullRequestTriggerTest;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

/**
 *
 */
public class MockIssueService extends IssueService {
    public MockIssueService(GitHubClient gitHubClient) {

    }
}
