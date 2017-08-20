package co.kenrg.mega.frontend.typechecking.errors;

public class UnknownIdentifierError extends TypeCheckerError {
    public final String name;

    public UnknownIdentifierError(String name) {
        this.name = name;
    }

    @Override
    public String message() {
        return String.format("Unknown identifier: %s", this.name);
    }
}
