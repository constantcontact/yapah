package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Comment;

/**
 *
 */
public class MockComment extends Comment {
    String commentBody;
    Long id;

    public MockComment(Long id, String commentBody) {
        this.commentBody = commentBody;
        this.id = id;
    }

    @Override
    public String getBody() {
        return commentBody;
    }

    @Override
    public long getId() {
        return id;
    }
}
