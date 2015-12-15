package com.constantcontact.plugins.pullrequestvalidation;

import java.io.PrintStream;

/**
 *
 */
public class LogWriter {
    private PrintStream logger;

    public LogWriter() {
    }

    public LogWriter(PrintStream logger) {
        this.logger = logger;
    }

    public void log(String msg) {
        logger.println(msg);
    }

    protected PrintStream getLogger() {
        return logger;
    }
}
