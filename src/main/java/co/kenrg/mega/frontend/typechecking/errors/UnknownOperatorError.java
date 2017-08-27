package co.kenrg.mega.frontend.typechecking.errors;

public class UnknownOperatorError extends TypeCheckerError {
    public final String operator;

    public UnknownOperatorError(String operator) {
        this.operator = operator;
    }

    @Override
    public String message() {
        return String.format("Unknown operator: %s", this.operator);
    }
}
