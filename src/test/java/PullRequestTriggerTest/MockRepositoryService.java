package PullRequestTriggerTest;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.util.ArrayList;

/**
 *
 */
public class MockRepositoryService extends RepositoryService {

    private ArrayList<String> repoInfo = new ArrayList<String>();


    public MockRepositoryService(GitHubClient gitHubClient) {

    }

    public Repository getRepository(String repoOwner, String repoName) {
        getRepoInfo().add(repoOwner);
        getRepoInfo().add(repoName);
        Repository repo = new Repository();
        User user = new User();
        user.setName("name");
        user.setLogin("login");
        repo.setOwner(user);
        repo.setName("name");
        return repo;
    }

    public ArrayList<String> getRepoInfo() {
        return repoInfo;
    }

}
