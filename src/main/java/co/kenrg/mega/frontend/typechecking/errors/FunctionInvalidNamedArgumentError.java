package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class FunctionInvalidNamedArgumentError extends TypeCheckerError {
    public final String argument;

    public FunctionInvalidNamedArgumentError(String argument, Position position) {
        super(position);
        this.argument = argument;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; invalid named argument: %s", this.argument);
    }
}

