package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class FunctionWithDefaultParamValuesArityError extends TypeCheckerError {
    public final int expectedArity;
    public final int actualArity;

    public FunctionWithDefaultParamValuesArityError(int expectedArity, int actualArity, Position position) {
        super(position);
        this.expectedArity = expectedArity;
        this.actualArity = actualArity;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke function; must provide at least %d params, got %d", this.expectedArity, this.actualArity);
    }
}

