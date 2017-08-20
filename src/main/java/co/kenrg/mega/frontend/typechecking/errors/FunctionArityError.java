package co.kenrg.mega.frontend.typechecking.errors;

public class FunctionArityError extends TypeCheckerError {
    public final int expectedArity;
    public final int actualArity;

    public FunctionArityError(int expectedArity, int actualArity) {
        this.expectedArity = expectedArity;
        this.actualArity = actualArity;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; expected arity: %d, got %d", this.expectedArity, this.actualArity);
    }
}

