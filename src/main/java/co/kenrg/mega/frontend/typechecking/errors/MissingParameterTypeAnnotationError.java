package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class MissingParameterTypeAnnotationError extends TypeCheckerError {
    private final String paramName;

    public MissingParameterTypeAnnotationError(String paramName, Position position) {
        super(position);

        this.paramName = paramName;
    }

    @Override
    public String message() {
        return String.format("Missing required type annotation on parameter: %s", this.paramName);
    }
}
