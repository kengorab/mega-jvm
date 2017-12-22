package co.kenrg.mega.repl.commands;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;

import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.backend.evaluation.evaluator.Environment;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

public class Commands {
    private static final HelpCommand helpCommand = new HelpCommand();
    private static final TypeDetailsCommand typeDetailsCommand = new TypeDetailsCommand();

    static final List<ReplCommand> ALL = Lists.newArrayList(helpCommand, typeDetailsCommand);

    static final Map<String, ReplCommand> ALL_COMMANDS = ALL.stream()
        .flatMap(replCommand -> replCommand.commands().stream().map(cmd -> Pair.of(cmd, replCommand)))
        .collect(toMap(Pair::getKey, Pair::getValue));

    private static final List<String> commands = Lists.newArrayList(ALL_COMMANDS.keySet());

    public static boolean shouldHandleLine(String input) {
        return commands.stream().anyMatch(input::startsWith);
    }

    public static String handleLine(String input, TypeEnvironment typeEnvironment, Environment environment) {
        String[] split = input.split(" ", 2);
        String command = split[0];
        ReplCommand replCommand = ALL_COMMANDS.get(command);

        String commandArgs = input.replaceFirst(command, "").trim();
        return replCommand.execute(commandArgs, typeEnvironment, environment);
    }
}
