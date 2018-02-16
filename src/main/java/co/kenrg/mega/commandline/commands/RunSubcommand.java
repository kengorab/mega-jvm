package co.kenrg.mega.commandline.commands;

import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import co.kenrg.mega.commandline.iface.Subcommand;
import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class RunSubcommand implements Subcommand {

    @Override
    public String name() {
        return "run [filename]";
    }

    @Override
    public String desc() {
        return "Evaluates the Mega file passed as an argument";
    }

    @Override
    public Options opts() {
        return new Options()
            .addOption("h", "help", false, "Displays this help information, for the run subcommand");
    }

    @Override
    public boolean execute(CommandLine command) {
        if (command.hasOption('h') || command.getArgList().size() != 1) {
            return false;
        }

        String fileToLoad = command.getArgList().get(0);
        ReplSubcommand.evalFile(new Environment(), new TypeEnvironment(), fileToLoad);
        return true;
    }
}
