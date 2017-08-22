package co.kenrg.mega.frontend.typechecking.errors;

public class ParametrizableTypeArityError extends TypeCheckerError {
    public final String typeName;
    public final int expectedArity;
    public final int actualArity;

    public ParametrizableTypeArityError(String typeName, int expectedArity, int actualArity) {
        this.typeName = typeName;
        this.expectedArity = expectedArity;
        this.actualArity = actualArity;
    }

    @Override
    public String message() {
        return String.format(
            "Type %s accepts %d type argument%s, got %s",
            this.typeName,
            this.expectedArity,
            this.expectedArity == 1 ? "" : "s",
            this.actualArity
        );
    }
}
