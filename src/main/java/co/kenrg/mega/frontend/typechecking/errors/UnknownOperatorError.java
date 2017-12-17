package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnknownOperatorError extends TypeCheckerError {
    public final String operator;

    public UnknownOperatorError(String operator, Position position) {
        super(position);
        this.operator = operator;
    }

    @Override
    public String message() {
        return String.format("Unknown operator: %s", this.operator);
    }
}
