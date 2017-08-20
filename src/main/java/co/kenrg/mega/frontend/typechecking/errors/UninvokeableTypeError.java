package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class UninvokeableTypeError extends TypeCheckerError {
    public final MegaType uninvokeableType;

    public UninvokeableTypeError(MegaType uninvokeableType) {
        this.uninvokeableType = uninvokeableType;
    }

    @Override
    public String message() {
        return String.format("Cannot invoke type as function: %s", this.uninvokeableType);
    }
}
