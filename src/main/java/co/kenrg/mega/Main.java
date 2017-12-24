package co.kenrg.mega;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.repl.Repl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.tuple.Pair;

public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        if (args.length == 0) {
//            startRepl();
            return;
        }

        String subcommand = args[0];
        String[] tailArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (subcommand) {
            case "repl": {
                handleReplCommand(tailArgs);
                return;
            }
            case "run": {
                handleRunCommand(tailArgs);
                return;
            }
            case "compile": {
                handleCompileCommand(tailArgs);
                return;
            }
            default: {

            }
        }
    }

//    private static void startRepl() {
//        Environment env = new Environment();
//        TypeEnvironment typeEnv = new TypeEnvironment();
//        Repl.start(env, typeEnv);
//    }

    private enum Subcommand {
        RUN("run [filename]", "Evaluates the Mega file passed as an argument"),
        REPL("repl", "Start up the Mega REPL (Read Eval Print Loop)"),
        COMPILE("compile [filename]", "Compile the Mega file passed as an argument to JVM class files");

        String name;
        String desc;

        Subcommand(String subcommandName, String subcommandDesc) {
            this.name = subcommandName;
            this.desc = subcommandDesc;
        }
    }

    private static CommandLine parse(Subcommand subcommand, Options opts, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            printUsage(subcommand, opts);
            System.exit(1);
            return null;
        }
    }

    private static void handleReplCommand(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("l", "load-file", true, "Loads a REPL, with the given file evaluated");
        opts.addOption("h", "help", false, "Displays this help information, for the repl subcommand");

        CommandLine command = parse(Subcommand.REPL, opts, args);
        assert command != null;

        if (command.hasOption('h') || !command.getArgList().isEmpty()) {
            printUsage(Subcommand.REPL, opts);
            return;
        }

        Environment env = new Environment();
        TypeEnvironment typeEnv = new TypeEnvironment();

        if (command.getOptionValue('l') == null) {
            Repl.start(env, typeEnv);
            return;
        }

        String fileToLoad = command.getOptionValue('l');
        evalFile(env, typeEnv, fileToLoad);

        Repl.start(env, typeEnv);
    }

    private static void evalFile(Environment env, TypeEnvironment typeEnv, String fileToLoad) {
        Path filepath = Paths.get(fileToLoad);
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            String code = new String(bytes);
            Repl.readEvalPrint(code, env, typeEnv);
        } catch (IOException e) {
            System.err.printf("No such file: %s\n", filepath.toAbsolutePath().toString());
            System.exit(1);
        }
    }

    private static void handleRunCommand(String[] args) throws ParseException {
        Options opts = new Options();
        opts.addOption("h", "help", false, "Displays this help information, for the run subcommand");

        CommandLine command = parse(Subcommand.RUN, opts, args);
        assert command != null;

        if (command.hasOption('h') || command.getArgList().size() != 1) {
            printUsage(Subcommand.RUN, opts);
            return;
        }

        String fileToLoad = command.getArgList().get(0);
        evalFile(new Environment(), new TypeEnvironment(), fileToLoad);
    }

    private static void handleCompileCommand(String[] args) throws ParseException, IOException {
        Options opts = new Options();
        opts.addOption("h", "help", false, "Displays this help information, for the compile subcommand");
        opts.addOption("o", "out-dir", true, "Directory where compiled class files should be written (defaults to current directory)");

        CommandLine command = parse(Subcommand.COMPILE, opts, args);
        assert command != null;

        if (command.hasOption('h') || command.getArgList().size() != 1) {
            printUsage(Subcommand.COMPILE, opts);
            return;
        }

        String outputDirectory = System.getProperty("user.dir");
        if (command.hasOption('o')) {
            Path path = Paths.get(command.getOptionValue('o'));
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            outputDirectory = path.toAbsolutePath().toString();
        }

        String fileToCompile = command.getArgList().get(0);
        Path filepath = Paths.get(fileToCompile);
        String code;
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            code = new String(bytes);
        } catch (IOException e) {
            System.err.printf("No such file: %s\n", filepath.toAbsolutePath().toString());
            System.exit(1);
            return;
        }

        TypeEnvironment typeEnv = new TypeEnvironment();
        Optional<Module> module = Repl.readAndTypecheck(code, typeEnv);
        if (!module.isPresent()) {
            System.out.println("Cannot proceed due to errors.");
            return;
        }

        Compiler compiler = new Compiler("MyClass");
        List<Pair<String, byte[]>> classes = compiler.compile(module.get());
        for (Pair<String, byte[]> output : classes) {
            String name = output.getLeft();
            byte[] bytes = output.getRight();

            File file = Paths.get(outputDirectory, String.format("%s.class", name)).toFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
        }
    }

    private static void printUsage(Subcommand subcommand, Options opts) {
        HelpFormatter usage = new HelpFormatter();
        usage.printHelp("mega " + subcommand.name, subcommand.desc, opts, "");
    }
}
