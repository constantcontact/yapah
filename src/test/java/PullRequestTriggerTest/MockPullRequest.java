package PullRequestTriggerTest;

import org.eclipse.egit.github.core.PullRequest;

/**
 *
 */
public class MockPullRequest extends PullRequest {

    @Override
    public int getNumber() {
        return 2;
    }
}
