package com.constantcontact.plugins.pullrequestvalidation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 *
 */
public class Logger {
    private File rootDir;
    private PrintStream logger;

    public Logger(File rootDir, PrintStream logger) {
        this.logger = logger;
        this.rootDir = rootDir;
    }

    protected void log(String msg) {
        logger.println(msg);
    }

    public File getLogFile() throws IOException {
        File file = new File(rootDir, "PR-Validator.log");
        if (! file.exists()) {
            if (! file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
        }
        return file;
    }


}
