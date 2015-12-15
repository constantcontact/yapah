# YAPah - Yet Another Pull Request
## Jenkins Plugin
### Summary
This Jenkins plugin builds pull requests from Github and comments on the pull request when the job has started and again to let you know the status when it finished.  

### Required Jenkins Plugins

* GitHub API Plugin
* GitHub Plugin
* Git Plugin
* Credentials Plugin
* Plain Credentials Plugin

### Installation

1. Build the plugin (mvn package)
2. Copy YAPah.hpi from your target directory into your plugins directory
3. Restart Jenkins

### Configuration

1. Go to Manage Jenkins
2. Configure System
3. YAPah Github Pull Request Poller
4. Change github.com to any enterprise github (github.company.com)

### Build Configuration

1. Create a parameterized build
2. Create a String parameter with the name of sha and the default value of master
3. Create a String parameter with the name of gitHubHeadRepository and default it to your repository
4. Select Git as your Source Code Management
5. Enter ${gitHubHeadRepository} for the Repository URL
6. Enter ${sha} for Branches to Build
7. Select YAPah Github Pull Request Poller under Build Triggers
8. Enter a Cron Schedule to your preference (e.g. * * * * * for once a minute)
9. Enter your Github Repository
10. Enter the owners name of the repository
11. Enter the Repository Name
12. Store a user to whom has access to comment on pull requests for the repository.  Please try to use a System user as passwords tend to expire.
13. Click Test Connection to validate that everything is setup correctly.
14. Add a Post build Action and select YAPah Github API Pull Request Validator

![alt tag](https://github.roving.com/rdavis/pull-request-validation-plugin/master/flow-diagram.png)

