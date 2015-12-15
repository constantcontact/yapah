package PullRequestTriggerTest;

import com.constantcontact.plugins.pullrequestvalidation.LogWriter;

import java.util.ArrayList;

/**
 *
 */
public class MockLogWriter extends LogWriter {

    private ArrayList<String> logEntries = new ArrayList<String>();

    public MockLogWriter() {

    }

    @Override
    public void log(String msg) {
        getLogEntries().add(msg);
    }

    public ArrayList<String> getLogEntries() {
        return logEntries;
    }
}
