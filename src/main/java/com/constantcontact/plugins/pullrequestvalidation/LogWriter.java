package com.constantcontact.plugins.pullrequestvalidation;

import java.io.PrintStream;

/**
 *
 */
public class LogWriter {
    private PrintStream logger;

    public LogWriter(PrintStream logger) {
        this.logger = logger;
    }

    protected void log(String msg) {
        logger.println(msg);
    }

    protected PrintStream getLogger() {
        return logger;
    }
}
