package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.typechecking.types.MegaType;

public class UnindexableTypeError extends TypeCheckerError {
    public final MegaType type;

    public UnindexableTypeError(MegaType type) {
        this.type = type;
    }

    @Override
    public String message() {
        return String.format("Cannot index into type: %s", this.type.signature());
    }
}

