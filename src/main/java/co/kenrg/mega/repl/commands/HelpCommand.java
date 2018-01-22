package co.kenrg.mega.repl.commands;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.List;

import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import com.google.common.collect.Lists;

public class HelpCommand implements ReplCommand {
    @Override
    public String description() {
        return "Display the help screen for [command], or this screen by default";
    }

    @Override
    public String detailedDescription() {
        return "Display high-level information about Mega REPL commands.\n" +
            "You can view deeper details about REPL commands by providing a command to the help\n" +
            "command, e.g. :h :h (which displays this message).";
    }

    @Override
    public List<String> commands() {
        return Lists.newArrayList(":h", ":help");
    }

    @Override
    public List<String> commandArgs() {
        return Lists.newArrayList("command");
    }

    @Override
    public String execute(String input, TypeEnvironment typeEnvironment, Environment environment) {
        if (input.isEmpty()) {
            String allCommands = Commands.ALL.stream()
                .map(replCommand -> {
                    String opts = String.join(", ", replCommand.commandArgs());
                    String line1 = replCommand.commands().stream()
                        .map(cmd -> String.format("%s [%s]", cmd, opts))
                        .collect(joining(", "));
                    return line1 + "\n  " + replCommand.description();
                })
                .collect(joining("\n"));
            return "Mega REPL commands:\n" + allCommands;
        }

        ReplCommand replCommand = Commands.ALL_COMMANDS.get(input);
        if (replCommand == null) {
            return "Unrecognized command: " + input;
        }

        return "" +
            "Detailed help information for " + input + "\n" +
            Arrays.stream(replCommand.detailedDescription().split("\n"))
                .collect(joining("\n  ", "  ", ""));
    }
}
