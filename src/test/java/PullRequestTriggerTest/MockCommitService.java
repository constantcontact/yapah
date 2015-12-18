package PullRequestTriggerTest;

import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class MockCommitService extends CommitService {
    Date commitDate = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();
    private IRepositoryIdProvider repository;
    private String sha;
    private CommitStatus commitStatus;

    public MockCommitService(GitHubClient gitHubClient) {
    }

    public void setCommitDate(Date commitDate) {
        this.commitDate = commitDate;
    }

    @Override
    public List<RepositoryCommit> getCommits(IRepositoryIdProvider repository, String sha, String path) {
        List<RepositoryCommit> commits = new ArrayList<RepositoryCommit>();
        RepositoryCommit rc1 = new RepositoryCommit();
        Commit cm = new Commit();
        CommitUser author = new CommitUser();
        author.setDate(commitDate);
        cm.setAuthor(author);
        rc1.setCommit(cm);

        commits.add(rc1);
        return commits;
    }


    @Override
    public CommitStatus createStatus(IRepositoryIdProvider repository, String sha, CommitStatus commitStatus)
            throws IOException {
        setRepository(repository);
        setSha(sha);
        setCommitStatus(commitStatus);
        return commitStatus;
    }


    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public CommitStatus getCommitStatus() {
        return commitStatus;
    }

    public void setCommitStatus(CommitStatus commitStatus) {
        this.commitStatus = commitStatus;
    }

    public IRepositoryIdProvider getRepository() {
        return repository;
    }

    public void setRepository(IRepositoryIdProvider repository) {
        this.repository = repository;
    }
}
