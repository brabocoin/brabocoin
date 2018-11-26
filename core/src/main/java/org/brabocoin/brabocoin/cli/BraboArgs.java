package org.brabocoin.brabocoin.cli;

import com.beust.jcommander.Parameter;

/**
 * CLI arguments for the Brabocoin application.
 */
public class BraboArgs {

    @Parameter(names = {"-c", "--config"}, description = "Path to configuration file")
    private String config;

    @Parameter(names = "--help", description = "Display this help message", help = true)
    private boolean help = false;

    public String getConfig() {
        return config;
    }

    public boolean isHelp() {
        return help;
    }
}
