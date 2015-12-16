package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import java.util.*;

/**
 *
 */
public class MockCommitService extends CommitService {
    Date commitDate = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();

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


}
