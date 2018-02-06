package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class UnsupportedFeatureError extends TypeCheckerError {
    public final String message;

    public UnsupportedFeatureError(String message, Position position) {
        super(position);
        this.message = message;
    }

    @Override
    public String message() {
        return String.format("Unsupported feature: %s", this.message);
    }
}

