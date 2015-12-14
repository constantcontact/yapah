# Pull Request Validator Jenkins Plugin
### Summary
The Pull Request Validator Jenkins Plugin was designed to easily create a way for a project to be built in jenkins after a PR request has been submitted.  The plugin consists of two components, trigger and a post action.  One will not work with out the other.  Setup of this plugin is fairly simple. 

Once Installed:
1. Goto Manage Jenkins
2. Goto Configure System
3. Look for Github Pull Request Poller and enter your jenkins domain, it must be at least 4 characters long and should not contain http:// or https://
4. Create or modify an existing job
5. Under Build Triggers, check off Github Pull Request Poller
6. Enter a cron time under the Cron Schedule (* * * * * for every minute)
7. Fill in the Github Repository, Owner, Name, System User and System User Password.  (System user needs to have privileges to comment on the repo and the keep in mind the password should not expire)
8. Do this for as many repositories as needed (Suggested 1 per job, various use cases account for multiple)
9. Do a build or whatever you need to do to validate your job
10. Add Post Action Build Step and select Github API Pull Request Validator (This requires no configuration)

What will happen now is when your pull request is submitted this job will find it and execute itself.  It will post to the pull request giving its status, a link and once completed a success / fail message giving the user the option to merge.
