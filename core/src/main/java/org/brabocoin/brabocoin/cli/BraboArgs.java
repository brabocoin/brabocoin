package org.brabocoin.brabocoin.cli;

import com.beust.jcommander.Parameter;
import org.brabocoin.brabocoin.util.Destructible;

/**
 * CLI arguments for the Brabocoin application.
 */
public class BraboArgs {

    @Parameter(names = {"-c", "--config"}, description = "Path to configuration file")
    private String config;

    @Parameter(names = {"-p", "--password"}, description = "Wallet unlocking password",
               password = true, converter = DestructibleCharArrayConverter.class)
    private Destructible<char[]> password;

    @Parameter(names = "--help", description = "Display this help message", help = true)
    private boolean help = false;

    public String getConfig() {
        return config;
    }

    public Destructible<char[]> getPassword() {
        return password;
    }

    public boolean isHelp() {
        return help;
    }
}
