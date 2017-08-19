package co.kenrg.mega.frontend.typechecking.errors;

public class UnknownTypeError extends TypeCheckerError {
    public final String typeName;

    public UnknownTypeError(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Unknown type %s", this.typeName);
    }
}
