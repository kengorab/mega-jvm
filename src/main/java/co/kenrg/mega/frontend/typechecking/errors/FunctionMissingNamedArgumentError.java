package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class FunctionMissingNamedArgumentError extends TypeCheckerError {
    public final String argument;

    public FunctionMissingNamedArgumentError(String argument, Position position) {
        super(position);
        this.argument = argument;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; missing named argument: %s", this.argument);
    }
}

