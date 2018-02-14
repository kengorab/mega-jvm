package co.kenrg.mega.frontend.typechecking.errors;

import co.kenrg.mega.frontend.token.Position;

public class VisibilityError extends TypeCheckerError {
    public final String module;
    public final String identName;

    public VisibilityError(String module, String identName, Position position) {
        super(position);
        this.module = module;
        this.identName = identName;
    }

    @Override
    public String message() {
        return String.format("Module %s has a value %s, but it is not exported", this.module, this.identName);
    }
}
