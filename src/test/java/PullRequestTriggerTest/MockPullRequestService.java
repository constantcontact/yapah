package PullRequestTriggerTest;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MockPullRequestService extends PullRequestService {
    private IRepositoryIdProvider repository;
    private String state;

    public MockPullRequestService(GitHubClient gitHubClient) {

    }

    @Override
    public List<PullRequest> getPullRequests(IRepositoryIdProvider repository, String state) {
        this.repository = repository;
        this.state = state;
        ArrayList prs = new ArrayList<MockPullRequest>();
        prs.add(new MockPullRequest());
        return prs;
    }

    public IRepositoryIdProvider getRepository() {
        return repository;
    }

    public String getState() {
        return state;
    }
}
