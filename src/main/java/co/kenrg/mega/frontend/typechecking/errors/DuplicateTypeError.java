package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class DuplicateTypeError extends TypeCheckerError {
    public final String typeName;

    public DuplicateTypeError(String typeName, Position position) {
        super(position);
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Duplicate type: %s already defined in this context", this.typeName);
    }
}
