package co.kenrg.mega.commandline.commands;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import co.kenrg.mega.commandline.iface.Subcommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class HelpSubcommand implements Subcommand {
    private List<Subcommand> subcommands;
    private PrintWriter pw;

    public void init(PrintStream ps, List<Subcommand> subcommands) {
        this.subcommands = subcommands;
        this.pw = new PrintWriter(ps);
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String desc() {
        return "Displays help information about various subcommands";
    }

    @Override
    public Options opts() {
        return new Options();
    }

    @Override
    public boolean execute(CommandLine command) {
        if (command.getArgList().size() == 0) {
            this.printTopLevelUsage();
            return true;
        }

        String topic = command.getArgList().get(0);
        Optional<Subcommand> helpTopic = subcommands.stream()
            .filter(c -> c.name().toLowerCase().startsWith(topic))
            .findFirst();
        if (!helpTopic.isPresent()) {
            return false;
        }

        this.printUsage(helpTopic.get());
        return true;
    }

    public void printUsage(Subcommand command) {
        HelpFormatter usage = new HelpFormatter();
        usage.printHelp(this.pw, 80, "mega " + command.name(), command.desc(), command.opts(), 2, 4, "");
        this.pw.flush();
    }

    public void printTopLevelUsage() {
        String usage = "" +
            "usage: mega\n" +
            "Mega has a couple of subcommands\n" +
            "  help              Displays this help information. You can also\n" +
            "                    run help on a specific command.\n" +
            "                        e.g. `mega help repl`\n" +
            "                        Equivalent to `mega repl -h`\n" +
            "\n" +
            "  repl              Starts up the Mega REPL (Read Eval Print Loop)\n" +
            "\n" +
            "  run               Executes the Mega file passed as an argument\n" +
            "\n" +
            "  compile           Compiles the Mega file to JVM bytecode";
        this.pw.println(usage);
        this.pw.flush();
    }
}
