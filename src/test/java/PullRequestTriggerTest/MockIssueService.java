package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import java.util.*;

/**
 *
 */
public class MockIssueService extends IssueService {
    public MockIssueService(GitHubClient gitHubClient) {

    }


    @Override
    public List<Comment> getComments(String repoOwner, String repoName, String issueNumber) {
        Date firstDate = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();
        Comment comment = new MockComment(1L, "foo", firstDate);
        ArrayList<Comment> comments = new ArrayList<Comment>();
        comments.add(comment);
        Date secondDate = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();
        Comment comment2 = new MockComment(2L, "~PR_VALIDATOR", secondDate);
        comments.add(comment2);
        Date thirdDate = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();
        Comment comment3 = new MockComment(3L, "foo~PR_VALIDATORfoo", thirdDate);
        comments.add(comment3);
        return comments;
    }
}
