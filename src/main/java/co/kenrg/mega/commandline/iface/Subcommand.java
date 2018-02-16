package co.kenrg.mega.commandline.iface;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface Subcommand {
    String name();

    String desc();

    Options opts();

    /**
     * Contains the logic represented by this subcommand
     *
     * @param command The result of parsing the command-line input (using the result of calling <code>opts()</code>)
     * @return true, if the command was handled properly; * false if a problem occurred and usage info should be displayed
     */
    boolean execute(CommandLine command);
}
