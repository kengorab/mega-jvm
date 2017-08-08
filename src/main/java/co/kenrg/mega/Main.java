package co.kenrg.mega;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import co.kenrg.mega.repl.Repl;
import co.kenrg.mega.repl.evaluator.Environment;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class Main {
    public static void main(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("r", "repl", false, "Evaluates the given file, and loads a REPL");

        CommandLineParser parser = new DefaultParser();
        CommandLine command;
        try {
            command = parser.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            printUsage(opts);
            System.exit(1);
            return;
        }
        List<String> argList = command.getArgList();

        if (argList.size() == 0) {
            Repl.start(new Environment());
            return;
        }

        if (argList.size() == 1) {
            Environment env = new Environment();
            Path filepath = Paths.get(argList.get(0));
            try {
                byte[] bytes = Files.readAllBytes(filepath);
                String code = new String(bytes);
                Repl.readEvalPrint(code, env);
            } catch (IOException e) {
                System.err.printf("No such file: %s\n", filepath.toAbsolutePath().toString());
                System.exit(1);
            }

            if (command.hasOption('r')) {
                Repl.start(env);
            }

            return;
        }

        printUsage(opts);
    }

    private static void printUsage(Options opts) {
        HelpFormatter usage = new HelpFormatter();
        usage.printHelp("mega [filename]", opts);
    }
}
