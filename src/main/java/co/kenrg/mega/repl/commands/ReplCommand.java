package co.kenrg.mega.repl.commands;

import java.util.List;

import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.repl.evaluator.Environment;

public interface ReplCommand {
    String description();

    String detailedDescription();

    List<String> commands();

    List<String> commandArgs();

    String execute(String input, TypeEnvironment typeEnvironment, Environment environment);
}
