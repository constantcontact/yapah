package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Comment;

import java.util.Date;

/**
 *
 */
public class MockComment extends Comment {
    String commentBody;
    Long id;
    Date createdAt;

    public MockComment(Long id, String commentBody, Date createdAt) {
        this.commentBody = commentBody;
        this.id = id;
        this.createdAt = createdAt;
    }

    @Override
    public String getBody() {
        return commentBody;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }
}
