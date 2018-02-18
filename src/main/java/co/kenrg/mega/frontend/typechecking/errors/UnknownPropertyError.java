package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownPropertyError extends TypeCheckerError {
    public final String propName;

    public UnknownPropertyError(String propName, Position position) {
        super(position);
        this.propName = propName;
    }

    @Override
    public String message() {
        return String.format("Unknown property: %s", this.propName);
    }
}
