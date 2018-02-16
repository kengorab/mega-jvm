package co.kenrg.mega;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import co.kenrg.mega.commandline.commands.CompileSubcommand;
import co.kenrg.mega.commandline.commands.HelpSubcommand;
import co.kenrg.mega.commandline.commands.ReplSubcommand;
import co.kenrg.mega.commandline.commands.RunSubcommand;
import co.kenrg.mega.commandline.iface.Subcommand;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class Main {
    private static final ReplSubcommand replSubcommand = new ReplSubcommand();
    private static final RunSubcommand runSubcommand = new RunSubcommand();
    private static final CompileSubcommand compileSubcommand = new CompileSubcommand();
    private static HelpSubcommand helpSubcommand = new HelpSubcommand();

    private static final List<Subcommand> subcommands = Lists.newArrayList(
        replSubcommand, runSubcommand, compileSubcommand, helpSubcommand
    );

    private static Subcommand getSubcommand(String subcommand) {
        switch (subcommand) {
            case "repl":
                return replSubcommand;
            case "run":
                return runSubcommand;
            case "compile":
                return compileSubcommand;
            case "-h":
            case "help":
                return helpSubcommand;
            default:
                return null;
        }
    }

    private static CommandLine parse(Subcommand subcommand, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(subcommand.opts(), args);
        } catch (UnrecognizedOptionException e) {
            helpSubcommand.printUsage(subcommand);
            System.exit(1);
            return null;
        }
    }

    @VisibleForTesting
    static void handleSubcommand(String[] args, PrintStream out) throws ParseException {
        // Late init help subcommand, to pass in PrintStream and subcommands list inc. help
        helpSubcommand.init(out, subcommands);

        if (args.length == 0) {
            helpSubcommand.printTopLevelUsage();
            return;
        }

        Subcommand subcommand = getSubcommand(args[0]);
        String[] tailArgs = Arrays.copyOfRange(args, 1, args.length);

        if (subcommand == null) {
            helpSubcommand.printTopLevelUsage();
            System.exit(1);
            return;
        }

        CommandLine command = parse(subcommand, tailArgs);
        boolean result = subcommand.execute(command);
        if (!result) {
            helpSubcommand.printUsage(subcommand);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws ParseException {
        handleSubcommand(args, System.out);
    }
}
