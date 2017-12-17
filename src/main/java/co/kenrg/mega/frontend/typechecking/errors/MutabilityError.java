package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class MutabilityError extends TypeCheckerError {
    public final String name;

    public MutabilityError(String name, Position position) {
        super(position);
        this.name = name;
    }

    @Override
    public String message() {
        return String.format("Cannot reassign to immutable binding: %s", this.name);
    }
}
