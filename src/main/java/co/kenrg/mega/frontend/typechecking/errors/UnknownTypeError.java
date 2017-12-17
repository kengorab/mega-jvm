package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownTypeError extends TypeCheckerError {
    public final String typeName;

    public UnknownTypeError(String typeName, Position position) {
        super(position);
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Unknown type %s", this.typeName);
    }
}
