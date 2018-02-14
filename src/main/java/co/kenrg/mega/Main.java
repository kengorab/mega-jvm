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

import co.kenrg.mega.backend.compilation.Compiler;
import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.frontend.ast.Module;
import co.kenrg.mega.frontend.typechecking.TypeCheckResult;
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
            printTopLevelUsage();
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
            case "-h":
            case "help": {
                handleHelpCommand(tailArgs);
            }
        }
    }

    private enum Subcommand {
        RUN(
            "run [filename]",
            "Evaluates the Mega file passed as an argument",
            new Options()
                .addOption("h", "help", false, "Displays this help information, for the run subcommand")
        ),
        REPL(
            "repl",
            "Start up the Mega REPL (Read Eval Print Loop)",
            new Options()
                .addOption("l", "load-file", true, "Loads a REPL, with the given file evaluated")
                .addOption("h", "help", false, "Displays this help information, for the repl subcommand")
        ),
        COMPILE(
            "compile [filename]",
            "Compile the Mega file passed as an argument to JVM class files",
            new Options()
                .addOption("h", "help", false, "Displays this help information, for the compile subcommand")
                .addOption("o", "out-dir", true, "Directory where compiled class files should be written (defaults to current directory)")
        );

        final String name;
        final String desc;
        final Options options;

        Subcommand(String subcommandName, String subcommandDesc, Options options) {
            this.name = subcommandName;
            this.desc = subcommandDesc;
            this.options = options;
        }
    }

    private static CommandLine parse(Subcommand subcommand, String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(subcommand.options, args);
        } catch (UnrecognizedOptionException e) {
            printUsage(subcommand);
            System.exit(1);
            return null;
        }
    }

    private static void handleReplCommand(String[] args) throws ParseException {
        CommandLine command = parse(Subcommand.REPL, args);
        assert command != null;

        if (command.hasOption('h') || !command.getArgList().isEmpty()) {
            printUsage(Subcommand.REPL);
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
        CommandLine command = parse(Subcommand.RUN, args);
        assert command != null;

        if (command.hasOption('h') || command.getArgList().size() != 1) {
            printUsage(Subcommand.RUN);
            return;
        }

        String fileToLoad = command.getArgList().get(0);
        evalFile(new Environment(), new TypeEnvironment(), fileToLoad);
    }

    private static String getFileContents(String moduleName) {
        String moduleFilepath = moduleName.endsWith(".meg") ? moduleName : moduleName + ".meg";
        Path filepath = Paths.get(moduleFilepath);
        try {
            byte[] bytes = Files.readAllBytes(filepath);
            return new String(bytes);
        } catch (IOException e) {
            System.err.printf("No such file: %s\n", filepath.toAbsolutePath().toString());
            System.exit(1);
            return null;
        }
    }

    private static TypeCheckResult<Module> compileModule(String fileToCompile, String outputDirectory) {
        Path filepath = Paths.get(fileToCompile);
        String code = getFileContents(fileToCompile);
        String moduleFileName = filepath.toString();

        TypeEnvironment typeEnv = new TypeEnvironment();
        Optional<TypeCheckResult<Module>> resultOpt = Repl.readAndTypecheck(code, typeEnv, moduleFileName, Main::getFileContents);
        if (!resultOpt.isPresent()) {
            System.err.println("Cannot proceed due to errors.");
            System.exit(1);
            return null;
        }
        TypeCheckResult<Module> result = resultOpt.get();
        Module module = result.node;

        String moduleName = filepath.getFileName().toString().replace(".meg", "");
        Compiler compiler = new Compiler(moduleName, typeEnv);
        compiler.setTypedModuleProvider(_moduleName -> compileModule(_moduleName, outputDirectory));
        List<Pair<String, byte[]>> classes = compiler.compile(module);
        for (Pair<String, byte[]> output : classes) {
            try {
                String name = output.getLeft();
                byte[] bytes = output.getRight();

                File file = Paths.get(outputDirectory, String.format("%s.class", name)).toFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bytes);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Could not write class files");
                System.exit(1);
                return null;
            }
        }

        return result;
    }

    private static void handleCompileCommand(String[] args) throws ParseException, IOException {
        CommandLine command = parse(Subcommand.COMPILE, args);
        assert command != null;

        if (command.hasOption('h') || command.getArgList().size() != 1) {
            printUsage(Subcommand.COMPILE);
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
        if (!fileToCompile.endsWith(".meg")) {
            System.err.printf("Invalid file extension for module %s; expected .meg extension\n", filepath.toAbsolutePath().toString());
            System.exit(1);
        }
        compileModule(fileToCompile, outputDirectory);
//        Path filepath = Paths.get(fileToCompile);
//        String code = getFileContents(fileToCompile);
//
//        String moduleFileName = filepath.toString();
//        if (!moduleFileName.endsWith(".meg")) {
//            System.err.printf("Invalid file extension for module %s; expected .meg extension\n", filepath.toAbsolutePath().toString());
//            System.exit(1);
//            return;
//        }
//
//        TypeEnvironment typeEnv = new TypeEnvironment();
//        Optional<TypeCheckResult<Module>> result = Repl.readAndTypecheck(code, typeEnv, moduleFileName, Main::getFileContents);
//        if (!result.isPresent()) {
//            System.err.println("Cannot proceed due to errors.");
//            System.exit(1);
//            return;
//        }
//        Module module = result.get().node;
//
//        String moduleName = filepath.getFileName().toString().replace(".meg", "");
//        Compiler compiler = new Compiler(moduleName, typeEnv);
//        compiler.setTypedModuleProvider(_moduleName -> {
//            String _code = getFileContents(_moduleName);
//            TypeEnvironment _typeEnv = new TypeEnvironment();
//            Optional<TypeCheckResult<Module>> _result = Repl.readAndTypecheck(_code, _typeEnv, _moduleName, Main::getFileContents);
//            if (_result.isPresent()) {
//                Compiler _compiler = new Compiler(_moduleName, _typeEnv);
//            }
//            return _result.orElse(null);
//        });
//        List<Pair<String, byte[]>> classes = compiler.compile(module);
//        for (Pair<String, byte[]> output : classes) {
//            String name = output.getLeft();
//            byte[] bytes = output.getRight();
//
//            File file = Paths.get(outputDirectory, String.format("%s.class", name)).toFile();
//            FileOutputStream fos = new FileOutputStream(file);
//            fos.write(bytes);
//            fos.close();
//        }
    }

    private static void handleHelpCommand(String[] args) {
        if (args.length == 0) {
            printTopLevelUsage();
            return;
        }

        String topic = args[0];
        Optional<Subcommand> helpTopic = Arrays.stream(Subcommand.values())
            .filter(c -> c.name().toLowerCase().equals(topic))
            .findFirst();
        if (!helpTopic.isPresent()) {
            printTopLevelUsage();
            return;
        }

        printUsage(helpTopic.get());
    }

    private static void printUsage(Subcommand subcommand) {
        HelpFormatter usage = new HelpFormatter();
        usage.setLeftPadding(2);
        usage.printHelp("mega " + subcommand.name, subcommand.desc, subcommand.options, "");
    }

    private static void printTopLevelUsage() {
        String usage = "" +
            "usage: mega\n" +
            "Mega has a couple of subcommands\n" +
            "  help              Displays this help information. You can also\n" +
            "                    run help on a specific command.\n" +
            "                      e.g. `mega help repl`\n" +
            "                      Equivalent to `mega repl -h`\n" +
            "  repl              Starts up the Mega REPL (Read Eval Print Loop)\n" +
            "  run               Executes the Mega file passed as an argument\n" +
            "  compile           Compiles the Mega file to JVM bytecode";
        System.out.println(usage);
    }
}
