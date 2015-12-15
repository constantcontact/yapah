package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MockIssueService extends IssueService {
    public MockIssueService(GitHubClient gitHubClient) {

    }


    @Override
    public List<Comment> getComments(String repoOwner, String repoName, String issueNumber) {
        Comment comment = new MockComment(1L, "foo");
        ArrayList<Comment> comments = new ArrayList<Comment>();
        comments.add(comment);
        Comment comment2 = new MockComment(2L, "~PR_VALIDATOR");
        comments.add(comment2);
        Comment comment3 = new MockComment(3L, "foo~PR_VALIDATORfoo");
        comments.add(comment3);
        return comments;
    }
}
