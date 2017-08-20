package co.kenrg.mega.repl.commands;

import java.util.List;

import co.kenrg.mega.frontend.typechecking.TypeEnvironment;
import co.kenrg.mega.frontend.typechecking.types.MegaType;
import co.kenrg.mega.repl.evaluator.Environment;
import com.google.common.collect.Lists;

public class TypeDetailsCommand implements ReplCommand {

    @Override
    public String description() {
        return "Provides type information about [ident]";
    }

    @Override
    public String detailedDescription() {
        return "Provides type information about an identifier in the current visible context.\n" +
            "For example, if you have run the following code in the REPL:\n" +
            "  let sum = (a: Int, b: Int) => a + b\n\n" +
            "then running `:t sum` will output:\n" +
            "  sum: (Int, Int) => Int\n\n" +
            "A warning will be displayed if no identifier in the current context matches.";
    }

    @Override
    public List<String> commands() {
        return Lists.newArrayList(":t", ":type");
    }

    @Override
    public List<String> commandArgs() {
        return Lists.newArrayList("ident");
    }

    @Override
    public String execute(String input, TypeEnvironment typeEnvironment, Environment environment) {
        String identifier = input.replace(":t ", "");
        MegaType type = typeEnvironment.get(identifier);
        if (type == null) {
            return String.format("Identifier %s unknown in this context", identifier);
        } else {
            return String.format("%s: %s", identifier, type.signature());
        }
    }
}
