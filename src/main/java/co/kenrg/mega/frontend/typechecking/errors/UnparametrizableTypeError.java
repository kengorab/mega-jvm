package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnparametrizableTypeError extends TypeCheckerError {
    public final String typeName;

    public UnparametrizableTypeError(String typeName, Position position) {
        super(position);
        this.typeName = typeName;
    }

    @Override
    public String message() {
        return String.format("Type %s does not accept type arguments", this.typeName);
    }
}
