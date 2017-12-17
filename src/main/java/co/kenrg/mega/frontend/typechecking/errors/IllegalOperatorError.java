package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;
import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class IllegalOperatorError extends TypeCheckerError {
    public final String operator;
    public final MegaType left;
    public final MegaType right;

    public IllegalOperatorError(String operator, MegaType left, MegaType right, Position position) {
        super(position);
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String message() {
        return String.format("Illegal operand types for operator: %s %s %s", this.left.signature(), this.operator, this.right.signature());
    }
}
