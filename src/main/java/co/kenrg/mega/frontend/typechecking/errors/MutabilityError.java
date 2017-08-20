package co.kenrg.mega.frontend.typechecking.errors;

public class MutabilityError extends TypeCheckerError {
    public final String name;

    public MutabilityError(String name) {
        this.name = name;
    }

    @Override
    public String message() {
        return String.format("Cannot reassign to immutable binding: %s", this.name);
    }
}
