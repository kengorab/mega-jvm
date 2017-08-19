package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class TypeMismatchError extends TypeCheckerError {
    public final MegaType expected;
    public final MegaType actual;

    public TypeMismatchError(MegaType expected, MegaType actual) {
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String message() {
        return String.format("Expected %s, got %s", this.expected.displayName(), this.actual.displayName());
    }
}
