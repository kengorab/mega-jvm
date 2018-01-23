package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class FunctionDuplicateNamedArgumentError extends TypeCheckerError {
    public final String argument;

    public FunctionDuplicateNamedArgumentError(String argument, Position position) {
        super(position);
        this.argument = argument;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; duplicate named argument: %s", this.argument);
    }
}

