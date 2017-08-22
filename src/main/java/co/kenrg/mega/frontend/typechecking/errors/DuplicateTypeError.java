package co.kenrg.mega.frontend.typechecking.errors;

public class DuplicateTypeError extends TypeCheckerError {
    public final String typeName;

    public DuplicateTypeError(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Duplicate type: %s already defined in this context", this.typeName);
    }
}
