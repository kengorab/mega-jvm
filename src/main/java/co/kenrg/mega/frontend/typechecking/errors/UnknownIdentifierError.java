package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownIdentifierError extends TypeCheckerError {
    public final String name;

    public UnknownIdentifierError(String name, Position position) {
        super(position);
        this.name = name;
    }

    @Override
    public String message() {
        return String.format("Unknown identifier: %s", this.name);
    }
}
