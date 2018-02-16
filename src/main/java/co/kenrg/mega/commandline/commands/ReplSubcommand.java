package co.kenrg.mega.commandline.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.commandline.iface.Subcommand;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.repl.Repl;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class ReplSubcommand implements Subcommand {

    @Override
    public String name() {
        return "repl";
    }

    @Override
    public String desc() {
        return "Start up the Mega REPL (Read Eval Print Loop)";
    }

    @Override
    public Options opts() {
        return new Options()
            .addOption("l", "load-file", true, "Loads a REPL, with the given file evaluated")
            .addOption("h", "help", false, "Displays this help information, for the repl subcommand");
    }

    @Override
    public boolean execute(CommandLine command) {
        if (command.hasOption('h') || !command.getArgList().isEmpty()) {
            return false;
        }

        Environment env = new Environment();
        TypeEnvironment typeEnv = new TypeEnvironment();

        if (command.getOptionValue('l') == null) {
            Repl.start(env, typeEnv);
            return true;
        }

        String fileToLoad = command.getOptionValue('l');
        evalFile(env, typeEnv, fileToLoad);

        Repl.start(env, typeEnv);
        return true;
    }

    static void evalFile(Environment env, TypeEnvironment typeEnv, String fileToLoad) {
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
}
