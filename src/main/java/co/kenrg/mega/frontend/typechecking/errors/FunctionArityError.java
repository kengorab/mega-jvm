package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class FunctionArityError extends TypeCheckerError {
    public final int expectedArity;
    public final int actualArity;

    public FunctionArityError(int expectedArity, int actualArity, Position position) {
        super(position);
        this.expectedArity = expectedArity;
        this.actualArity = actualArity;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; expected arity: %d, got %d", this.expectedArity, this.actualArity);
    }
}

